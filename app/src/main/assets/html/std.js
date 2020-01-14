if (navigator.userAgent.toLowerCase().indexOf(' line/') > -1) {
	if (navigator.userAgent.indexOf('Android') > -1) {
		location.replace('intent://' + location.host + '#Intent;scheme=' + location.protocol.replace(':', '') + ';package=com.android.chrome;end');
	//} else {
	//	location.replace(location.origin + location.pathname.replace(new RegExp("(?:\\\/+[^\\\/]*){0,1}$"), "/") + data['path']);
	}
}

var lp_time = 500;
var lp_timer = null;
var lp_startX;
var lp_startX;
$(window).on("touchstart", function(e) {
	if (lp_timer)
		clearTimeout(lp_timer);

	lp_timer = setTimeout(function() {
		$(e.target).trigger('longpress');
	}, lp_time);
}).on("touchmove", function(e) {
	if (lp_timer == null) {
		lp_timer = setTimeout(function() {
			$(e.target).trigger('longpress');
		}, lp_time);
	} else {
		clearTimeout(lp_timer);
		lp_timer = null;
	}
}).on("touchend", function() {
	clearTimeout(lp_timer);
	lp_timer = null;
});


var sel_mode = 0;
var sel_pos = 0;
var sel_list = [];


$(window).on('load', function() {
	$('.simg').on('load', function() {
		//$(this).css('padding', '0').css('height', 'auto').animate({opacity: '1'}, 300);
		$(this).css('padding', '0').css('height', 'auto').css('opacity', '1');
	});

    $('.simg').on('error', function() {
        setTimeout(function() {
		    $(this).attr('src', '');
		    $(this).attr('src', $(this).attr('s'));
        }, 1000);
    });

	$('.simg').each(function() {
		$(this).attr('src', $(this).attr('s'));
	});
});

$(function() {
	$(window).on('resize', function() {
		var body = $('html').width() - 20 - 6;
		var cou = Math.floor(body / 210);

		if ((img_list.length + 1) * 210 < body) {
			$('#lightbox-margin').css('width', 0);
			$('#lightbox-margin-table').css('margin', 'auto');
		} else {
			$('#lightbox-margin').css('width', Math.floor((body - cou * 210) / 2) + 'px');
			$('#lightbox-margin-table').css('margin', '0');
		}
	});
	$(window).resize();
	$('#lightbox-margin-table').css('opacity','1');

	$('.simg').on('click', function() {

		now = img_list.indexOf($(this).attr('title'));
		$('.sbox:nth-of-type(' + (now + 1) + ')').css('border', 'solid 2px #8af');
		$('.sbox:nth-of-type(' + (img_pos + 1) + ')').css('border', 'solid 2px #666');
		img_pos = now;

		if (sel_mode > 0) {
			var title = $(this).attr('title');
			var now = sel_list.indexOf(title);
			sel_pos = now;

			// 複数選択
            if (sel_list.indexOf(title) > -1) {
                sel_list.splice(sel_pos, 1);
                $(this).closest('.sbox').find('div').css('background-color', '#000');
            } else {
                sel_list.push(title);
                $(this).closest('.sbox').find('div').css('background-color', '#79f');
            }

            if (sel_list.length == 0) {
                sel_mode = 0;
                $('.bbox').css('background-color', '#000').css('opacity', '0');
                $('#dlpanel').css('display', 'none');
            }

            return false;
        } else {
			var now = sel_list.indexOf($(this).attr('title')) + 1;
			sel_pos = now;
        }
	});

	$('.sbox').on('longpress', function(e) {
		if (sel_mode == 0) {
			/*if (navigator.userAgent.indexOf('iPhone') > -1 || navigator.userAgent.indexOf('iPad') > -1 || navigator.userAgent.indexOf('iPod') > -1) {
				// iPhoneはDL
		        var link = document.createElement('a');
		        document.body.appendChild(link);
		        link.href = $(this).attr('url').replace('data/', 'data/d/');
		        link.click();
		        document.body.removeChild(link);
			} else {*/
				// Androidは複数選択
				sel_list = [];
				sel_mode = 1;
				$('.bbox').css('background-color', '#000').css('opacity', '0.5');
				$('.sbox:nth-of-type(' + sel_pos + ')').css('border', 'solid 2px #666');
				$('#dlpanel').css('display', 'block');

				// 1つ目を選択
				$(this).find('img').click();
			//}

			e.preventDefault();
			return false;
		}
    });

	$('.bbox').on('click',function() {

		$(this).parent('div').find('img').click();
	});

	$('#sel_cancel').on('click', function(e) {
		sel_mode = 0;
		sel_list = [];
		$('.bbox').css('background-color', '#000').css('opacity', '0');
		$('#dlpanel').css('display', 'none');

		return false;
	});

/*
	$('#sel_reset').on('click', function() {
		sel_list = [];
		$('.bbox').css('background-color', '#000').css('opacity', '0.5');

		return false;
	});
*/
	$('#sel_dl').on('click', function() {
		if (sel_list.length == 0)
			return false;

		$('#dlpanel').css('display', 'none');
		$('#dlpanel_e').css('display', 'block');
		setTimeout(function() {
			$('#dlpanel_e').animate({opacity: '0'}, 500, function() {
				$('#dlpanel_e').css('display', 'none').css('opacity', '1');
			});
		}, 3000);

		var arg  = [];
		url = location.search.substring(1).split('&');
		for(i=0; url[i]; i++) {
			var k = url[i].split('=');
			arg[k[0]] = k[1];
		}

		$('.bbox').css('background-color', '#000').css('opacity', '0');

		$.each(sel_list, function(index, value) {
			var link = document.createElement('a');
			document.body.appendChild(link);
			link.href = location.origin + location.pathname + 'data/d/' + value;
			link.download = value;
			link.click();
			document.body.removeChild(link);
	    });

		sel_list = [];
		sel_mode = 0;
		return false;
	});

	$('.lightbox a').simpleLightbox({sourceAttr: 'm', fileExt: 'jpg|jpeg|png|bmp', doubleTapZoom: 2, animationSpeed: 150, overlay:true, alertErrorMessage: ''})
	.on('changed.simplelightbox', function() {
		var title = $('.sl-image').find('img').attr('src');
		title = title.substring(title.lastIndexOf('/') + 1);
		var now = img_list.indexOf(title);
		$('.sbox:nth-of-type(' + (now + 1) + ')').css('border', 'solid 2px #8af');
		$('.sbox:nth-of-type(' + (img_pos + 1) + ')').css('border', 'solid 2px #666');
		img_pos = now;
	})
	.on('shown.simplelightbox', function() {
		/*if (navigator.userAgent.indexOf('iPhone') > -1 || navigator.userAgent.indexOf('iPad') > -1 || navigator.userAgent.indexOf('iPod') > -1) {
			$('.sl-navigation').append('<div class="sl-org">ORG</div>');
		} else {*/
			$('.sl-navigation').append('<div class="sl-org">ORG</div><div class="sl-dl">DL</div>');

			$('.sl-dl').off('click');
			$('.sl-dl').on('click',function(e) {
				var dl_url = $('.sl-image').parent().find('img').attr('src');
				location.href = 'data/d/' + dl_url.substring(7);
				e.stopPropagation();
				return false;
			});
		//}
		var url = $('.sl-image').parent().find('img').attr('src');
		if (url.substr(0, 3) == 'qr/') {
			$('.sl-org').css('display', 'none');
			$('.sl-dl').css('display', 'none');
		} else {
			$('.sl-org').css('display', 'block');
			$('.sl-dl').css('display', 'block');
		}

		$('.sl-org').off('click');
		$('.sl-org').on('click',function(e) {
			var org_url = $('.sl-image').parent().find('img').attr('src');
			window.open('data/' + org_url.substring(7));
			e.stopPropagation();
			return false;
		});
	});
});

