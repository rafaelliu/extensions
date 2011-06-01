// JavaScript Document
var size = 65;

function accessibilidade(i) {
	size = Math.max(60, Math.min(85, (size + i*5)));
	$j("body").css("font-size", size + "%");
}

var nodes = eXo.env.portal.navigations[0].nodes;
var selectedNodeUri = eXo.env.portal.selectedNodeUri;
var baseUri = eXo.env.portal.context + '/' + eXo.env.portal.accessMode + '/' + eXo.env.portal.portalName + '/';

function createMarkup(provider, opened) {
	var opened = opened ? "block" : "none",
		// str = "<ul style='display:" + opened + "'>",
                str = "<ul>",
		nav = selectedNodeUri.split("/");
	for (var x=0; x<provider.length; x++) {
			str += "<li><a href='" + baseUri + provider[x].uri + "'><span>" + provider[x].resolvedLabel + "</span></a>";
			opened = provider[x].uri == selectedNodeUri;
			if (provider[x].nodes != null)
				str += createMarkup(provider[x].nodes, opened);
			str += "</li>";
	}
	str += "</ul>";
	return str;
}



var markup = createMarkup(nodes, true);

$j(document).ready(function(){
	$j("#menu").append(markup);
});

$j(function(p){

	$j(".search").doSearch({message: "Busca no menu"});

}); 
