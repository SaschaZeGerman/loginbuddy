/*
 This script is part of /callback to turn URL fragments into queries. This way loginbuddy can handle parameters that otherwise would be handled by a javascript client.
 If there is a better solution I am open ...
 */
if (location.href.indexOf('#') >= 0) {
    if (location.search) {
        window.location = location.href.replace('#', '&handled=true&');
    } else {
        window.location = location.href.replace('#', '?handled=true&');
    }
} else {
    if (location.search) {
        window.location = location.href + '&handled=true';
    } else {
        window.location = location.href + '?handled=true';
    }
}