function colorFix() {
	
	var selection = '<div class="WordSection1">';
	var replacement = '<div class="margin" id="leftMargin"></div><div class="margin" id="rightMargin"></div><div class="WordSection1">';
	document.body.innerHTML = document.body.innerHTML.replace(selection, replacement);
	
	var selection = 'darkblue"';
	var replacement = 'none" class="code"';
	while (document.body.innerHTML.includes(selection)) {
		document.body.innerHTML = document.body.innerHTML.replace(selection, replacement);
	}
	var selection = 'navy"';
	var replacement = 'none" class="code"';
	while (document.body.innerHTML.includes(selection)) {
		document.body.innerHTML = document.body.innerHTML.replace(selection, replacement);
	}
	
	selection = '#BFBFBF';
	replacement = '#888; text-decoration:line-through';
	while (document.body.innerHTML.includes(selection)) {
		document.body.innerHTML = document.body.innerHTML.replace(selection, replacement);
	}
	
	selection = '<s>';
	replacement = '';
	while (document.body.innerHTML.includes(selection)) {
		document.body.innerHTML = document.body.innerHTML.replace(selection, replacement);
	}
	
	selection = 'lightgrey';
	replacement = '#111; color: white !important; font-family: chalk; padding-left: 6px; padding-right: 6px; padding-bottom: 1px;';
	while (document.body.innerHTML.includes(selection)) {
		document.body.innerHTML = document.body.innerHTML.replace(selection, replacement);
	}
	selection = 'silver';
	replacement = '#111; color: white !important; font-family: chalk; padding-left: 6px; padding-right: 6px; padding-bottom: 1px;';
	while (document.body.innerHTML.includes(selection)) {
		document.body.innerHTML = document.body.innerHTML.replace(selection, replacement);
	}	
}

window.onload = colorFix;

document.head.innerHTML = document.head.innerHTML + '<style>a {color:rgba(0,0,0,0) !important;}</style>';