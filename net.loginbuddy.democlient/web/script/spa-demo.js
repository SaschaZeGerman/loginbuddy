/**
 * This is a very simple javascript based oauth client (public client). The main idea is to show steps that are executed between initiating an
 * authorization flow and receiving an issued access_token.
 *
 * Do not use this code for anything else than 'evaluation purposes'!
 *
 * I am by no means a javascript expert and therefore you may think jay or nay.
 *
 * I hope this helps understanding the basics of oauth flows (in this case, the authorization_code flow).
 *
 * NOTE: PKCE is not used yet, still have to figure out how to generate a valid SHA-256 value of 'code_verifier'
 *
 * Loginbuddy, April 2020
 */

/**
 * This client will handle four cases:
 *
 * 1. Initialization, the first time this scripts loads
 * 2. Handling a redirect response. The authorization server has responded either with an error or with an authorization code
 * 3. Exchanging the code for a token response
 * 4. Receiving the token response, clearing the current state
 *
 * The state object is a JSON object having this structure:
 *
 * key = null or a UUID
 * value = {"state":"...", "nonce":"...", "next":"..."}
 *
 * - state = empty string or a UUID
 * - nonce = empty string or a UUID, created with the authorization request
 * - next = empty string or /token, the next endpoint to call
 */

// always write the top part of the UI
// add any other content into the div 'divResponse'
document.write('<body>' +
    '<div class="container" id="content"><h1>Welcome to Loginbuddy-Democlient!</h1>\n' +
    '    <p>This is a demo client of the open source project <a href="https://github.com/SaschaZeGerman/loginbuddy" target="_blank"><strong>Loginbuddy</strong></a>.</p>\n' +
    '    <p>This client simulates a <strong>Single Page App (SPA)</strong> that a developer may build.</p>' +
    '    <p>Please note that this client is pretty simple. Check the comments in <strong>script/spa-demo.js</strong> to learn more about it. However, hopefully it does help to understand how an OAuth flow in a SPA works!</p>' +
    '    <hr/><div id="divResponse"></div>' +
    '</div></body>');

// assumption:
// - there is only a URL search component (query parameters) if an authorization flow is active
if (window.location.search.length > 0) {

    let params = window.location.search.substr(1).split('&');

    let title = '';
    let output = '';

    for (let i = 0; i < params.length; i++) {
        let key = params[i].split('=')[0];
        let value = decodeURI(params[i].split('=')[1]);
        output = output + '<strong>' + key + '</strong>: ' + value + '</br>';
    }

    /*
    /* this section is very specific. Other servers may respond with more or less parameters, Loginbuddy does not
     */

    // error: (state & error & error_description)
    if (params.length === 3) {
        title = 'Error!';
    }
    // success: code response (state & code)
    else if (params.length === 2) {
        title = 'Success!<br/>An authorization_code was received!<br/><small>(shown only for demo purposes!)</small>';
        output = output + '<button type="submit" class="btn btn-primary" onclick="return exchangeCode(\'' + params[0] + '\',\'' + params[1] + '\');">Exchange code for token response</button>';
    } else {
        // unknown: no valid parameter combination found
        title = 'Hmmm ... something unexpected: an unknown parameter combination:'
        output = window.location.href;
    }
    printFlowResponse(title, output);
}
// if not, simply print the 'get started' page
else {
    printFirstPage();
}

/**
 * OAuth
 *
 * Initiate the authorization_code flow. This is called with a click of a button by the user
 */
function authorize() {

    let provider = document.getElementById('provider').value;
    if (provider !== null && provider.length <= 24) {
        provider = '&provider=' + encodeURI(provider);
    } else {
        provider = '';
    }

    let state = generate_random_string(32);
    let nonce = generate_random_string(32);  // this will appear within the id_token, issued by the provider. And in Loginbuddys response

    let config = new ClientConfiguration();
    let authorizeUrl = config.getAuthServer() + '/authorize?'
        + 'client_id=' + encodeURI(config.getClientId())
        + '&redirect_uri=' + encodeURI(config.getRedirectUri())
        + '&scope=' + encodeURI(config.getScope())
        + '&response_type=' + encodeURI(config.getResponseType())
        + '&nonce=' + encodeURI(nonce)
        + provider
        + '&state=' + encodeURI(state);

    setSpaState(state, '{"state":"' + state + '", "nonce":"' + nonce + '", "next":"/token"}');

    window.location = authorizeUrl;
}

/**
 * OAuth
 *
 * This is called after the authorization server has issued an authorization code.
 *
 * This is only successful if CORS is enabled for the /token endpoint
 *
 * @param one, either "state=value" or "code=codeValue"
 * @param two, either "state=value" or "code=codeValue"
 */
function exchangeCode(one, two) {

    let code = '';
    let givenStateParameter = '';
    let stateInStorage = '';

    let keyValueOne = one.split('=');
    let keyValueTwo = two.split('=');

    if ('state' === keyValueOne[0]) {
        code = keyValueTwo[1];
        givenStateParameter = keyValueOne[1];
    } else {
        code = keyValueOne[1];
        givenStateParameter = keyValueTwo[1];
    }

    // compare the given state value to the one found as part of the session state in sessionStorage
    stateInStorage = getSpaState(givenStateParameter);
    if (stateInStorage == null) {
        clearSpaState();
        printFlowResponse('Error!', 'The given session is either expired or invalid!' + getMessageFooter());
    } else {
        stateInStorage = JSON.parse(stateInStorage);
        if (stateInStorage['state'] !== givenStateParameter) {
            clearSpaState();
            printFlowResponse('Error!', 'The given session is invalid!');
        } else {
            // Later, we need to check if this is found within the token response (id_token or provider, Loginbuddys response).
            // If not, the riven response was not meant for us
            let nonce = stateInStorage['nonce'];

            // Grab the next endpoint
            let next = stateInStorage['next'];

            clearSpaState();

            let config = new ClientConfiguration();
            let reqMsg = 'client_id=' + encodeURI(config.getClientId())
                + '&redirect_uri=' + encodeURI(config.getRedirectUri())
                + '&grant_type=' + encodeURI(config.getGrantType())
                + '&code=' + encodeURI(code);

            postMsg(config.getAuthServer() + next, nonce, reqMsg);
        }
    }
}

/**
 * Send a POST request
 * @param targetUrl In our case this is the /token endpoint
 * @param expectedNonce The nonce that should be found in the response. Finding this verifies that the response was made for our client and our session
 * @param msg The message to be send. All parameters need to be URLEncoded
 */
function postMsg(targetUrl, expectedNonce, msg) {
    $.ajax({
        type: 'POST',
        url: targetUrl,
        data: msg,
        dataType: 'json',
        contentType: 'application/x-www-form-urlencoded',
        async: false,
        success: function (data) {
            // validate that 'nonce' is found within the response. Ignoring that for now ...
            // assert(expectedNonce == data.details_provider.id_token_payload.nonce)
            // assert(expectedNonce == data.details_loginbuddy.nonce)
            printFlowResponse('Success!<br/>This is the token response including Loginbuddy specific details:', getPostMsgResponse(data));
        },
        error: function (data) {
            printFlowResponse('Error!<br/>Something went wrong!:', getPostMsgResponse(data));
        }
    });
}

/**
 * The configuration for our client
 */
class ClientConfiguration {

    constructor() {
        this.client_id = 'clientIdForTestingPurposes';
        this.redirect_uri = 'https://democlient.loginbuddy.net/spa.html';
        this.scope = 'openid email profile';
        this.responseType = 'code';
        this.authserver = 'https://local.loginbuddy.net';
        this.grant_type = 'authorization_code';
    }

    getClientId() {
        return this.client_id;
    }

    getRedirectUri() {
        return this.redirect_uri;
    }

    getScope() {
        return this.scope;
    }

    getAuthServer() {
        return this.authserver;
    }

    getResponseType() {
        return this.responseType;
    }

    getGrantType() {
        return this.grant_type;
    }
}

/*
 * As of here you can find helper methods
 */

function getSpaState(key) {
    return sessionStorage.getItem(key);
}

function setSpaState(key, value) {
    sessionStorage.setItem(key, value);
}

function clearSpaState() {
    sessionStorage.clear();
}

function getPostMsgResponse(data) {
    return '<code class="language-json">' + JSON.stringify(data, null, 2) + '</code>' + getMessageFooter();
}

function getMessageFooter() {
    return '<hr/><p>Try again! <a href="democlientApp.jsp"><strong>Web Application!</strong></a>, <a href="spa.html"><strong>SPA!</strong></a></p>'
}

function printFlowResponse(title, output) {
    document.getElementById('divResponse').innerHTML = '<h3>' + title + '</h3><pre>' + output + '</pre>';
    Prism.highlightAll(false, null);
}

function printFirstPage() {
    let content = '<p>Selecting <strong>Submit</strong> initiates an OAuth 2.0 authorization code flow. It takes you to Loginbuddy where you can select a social provider.</p>\n' +
        '    <p>In the end you will see which information about you is available and how it looks like.</p>' +
        '    <form>\n' +
        '        <div class="form-group">\n' +
        '            <label for="provider">Provider (type \'server_loginbuddy\' to skip the next screen. Leave it blank the first time you try)</label>\n' +
        '            <input type="text" id="provider" name="provider" class="form-control" size="80">\n' +
        '        </div>\n' +
        '        <button type="button" class="btn btn-primary" onclick="return authorize();">Submit</button>\n' +
        '    </form>' +
        '    <hr/>';
    document.getElementById('divResponse').innerHTML = content;
}

/**
 *
 * The random string method was copied from here:
 * https://codehandbook.org/generate-random-string-characters-in-javascript/
 *
 */
function generate_random_string(string_length) {
    let random_string = '';
    let random_ascii;
    let ascii_low = 65;
    let ascii_high = 90
    for (let i = 0; i < string_length; i++) {
        random_ascii = Math.floor((Math.random() * (ascii_high - ascii_low)) + ascii_low);
        random_string += String.fromCharCode(random_ascii)
    }
    return random_string
}

/**
 *
 *  Base64 encode / decode
 *  http://www.webtoolkit.info/
 *
 **/
var Base64 = {

// private property
    _keyStr: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

// public method for encoding
    encode: function (input) {
        var output = "";
        var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;

        input = Base64._utf8_encode(input);

        while (i < input.length) {

            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }

            output = output +
                this._keyStr.charAt(enc1) + this._keyStr.charAt(enc2) +
                this._keyStr.charAt(enc3) + this._keyStr.charAt(enc4);

        }

        return output;
    },

// public method for decoding
    decode: function (input) {
        var output = "";
        var chr1, chr2, chr3;
        var enc1, enc2, enc3, enc4;
        var i = 0;

        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        while (i < input.length) {

            enc1 = this._keyStr.indexOf(input.charAt(i++));
            enc2 = this._keyStr.indexOf(input.charAt(i++));
            enc3 = this._keyStr.indexOf(input.charAt(i++));
            enc4 = this._keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64) {
                output = output + String.fromCharCode(chr2);
            }
            if (enc4 != 64) {
                output = output + String.fromCharCode(chr3);
            }

        }

        output = Base64._utf8_decode(output);

        return output;

    },

// private method for UTF-8 encoding
    _utf8_encode: function (string) {
        string = string.replace(/\r\n/g, "\n");
        var utftext = "";

        for (var n = 0; n < string.length; n++) {

            var c = string.charCodeAt(n);

            if (c < 128) {
                utftext += String.fromCharCode(c);
            } else if ((c > 127) && (c < 2048)) {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            } else {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }

        }

        return utftext;
    },

// private method for UTF-8 decoding
    _utf8_decode: function (utftext) {
        var string = "";
        var i = 0;
        var c = c1 = c2 = 0;

        while (i < utftext.length) {

            c = utftext.charCodeAt(i);

            if (c < 128) {
                string += String.fromCharCode(c);
                i++;
            } else if ((c > 191) && (c < 224)) {
                c2 = utftext.charCodeAt(i + 1);
                string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
                i += 2;
            } else {
                c2 = utftext.charCodeAt(i + 1);
                c3 = utftext.charCodeAt(i + 2);
                string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
                i += 3;
            }

        }

        return string;
    }
}