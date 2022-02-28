function printLoginbuddyResponse() {
    let pre = document.getElementById('idLoginbuddyResponse')
    let content = pre.innerText;
    pre.innerHTML = '<code class="language-json">' + JSON.stringify(JSON.parse(content), null, 2) + '</code>';
    Prism.highlightAll(false, null);
}