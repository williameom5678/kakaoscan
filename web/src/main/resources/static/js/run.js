var checkInit = (function() {
    var count = 0;
    return function() {
        count++;
        return count === 1;
    }
})();

function refreshPage(queryString) {
    var newUrl = window.location.origin + window.location.pathname + "?phoneNumber=" + queryString;
    window.location.href = newUrl;
    return false;
}

function init(server) {
    if (!checkInit()) {
        return;
    }

    let startCallback = false;
    let tick = 0;
    let timeOutTick = 0;
    let failCount = 0;

    const target = $('#target_name');
    const targetStatusMessage = $('#target_status');
    const number = $('#number');
    const searchIcon = $('#search_ico');
    const spinner = $('#spinner');
    const spinnerText = $('#spinner_text');
    const results = $('#result');
    const tab1 = $('#tab1_res');
    const tab2 = $('#tab2_res');
    const tab3 = $('#tab3_res');
    const currentProfile = $('#current_profile img');
    const currentMusic = $('#current_profile_music');
    const currentMusicName = $('#current_profile_music strong');
    const currentArtistName = $('#current_profile_music p');
    const currentMusicAlbumUrl = $('#current_profile_music img');
    const moveTop = $('#move-top');
    const limitInfo = $('#limit_info');
    const queueInfo = $('#queue_info');
    const previewProfile = $('#preview_profile');
    const previewProfileImg = $('#preview_profile img');
    const helpInfo = $('#help_info');
    const footer = $('#footer');

    let socket;
    function connect() {
        socket = new WebSocket((document.location.protocol === "http:" ? 'ws://' : 'wss://') + server + '/kakaoscan');

        socket.onclose = function(event) {
            console.log('Socket is closed', event.reason);
            setTimeout(function() {
                connect();
            }, 1000);
        };
        socket.onerror = function(err) {
            console.error('Socket encountered error: ', err.message, 'Close socket');
            setTimeout(function() {
                connect();
            }, 1000);
        };
        socket.onmessage = function (event) {
            if (event.data.length > 0) {
                console.log(event.data);

                try {
                    let res = JSON.parse(event.data);

                    searchIcon.removeClass('hide');

                    if (res.hasOwnProperty('RemainingQueue')) {
                        // console.log(res.RemainingQueue);
                        spinnerText.text(res.RemainingQueue);
                    } else {
                        startCallback = false;

                        spinner.addClass('hide');

                        spinnerText.text('대기 인원: -');
                    }

                    if (res.hasOwnProperty('Error')) {
                        alert(res.Error.replace(/\\n/gi, '\n'));
                        socket.close();
                        refreshPage(number.val());
                        return;
                    }

                    // find user & message
                    else if (res.hasOwnProperty('OriginName')) {
                        spinnerText.text('대기 인원: -');

                        helpInfo.addClass('hide');

                        results.removeClass('hide');

                        let name = res.OriginName;
                        let statusMessage = res.StatusMessage;

                        if (name.length === 0) {
                            name = '이름 없음';
                        }
                        if (statusMessage.length === 0) {
                            statusMessage = '상태메세지 없음';
                        }

                        // current profile
                        target.text(decodeString(name));
                        targetStatusMessage.text(decodeString(statusMessage));

                        currentProfile.attr('src', '/img/empty_profile.png');
                        if (res.hasOwnProperty('ProfileImageUrl') && res.ProfileImageUrl.length > 0) {
                            currentProfile.attr('src', res.ProfileImageUrl);
                        }

                        if (res.hasOwnProperty('MusicName') && res.MusicName.length > 0 && res.hasOwnProperty('MusicAlbumUrl') && res.MusicAlbumUrl.length > 0) {
                            currentMusicName.text(res.MusicName);
                            currentArtistName.text(res.ArtistName);
                            currentMusicAlbumUrl.attr('src', res.MusicAlbumUrl.replace('http://', 'https://'));
                            currentMusic.removeClass('hide');
                        }

                        // preview
                        previewProfileImg.attr('src', res['Host'] + res['Tick'] + '/preview/preview.jpg?t=' + Math.random());

                        // find user profile image
                        render(tab1, res, 'ImageUrl', '.mp4.jpg', 'col-lg-4 b-4');

                        // find user bg image
                        render(tab2, res, 'BgImageUrl', '.mp4.jpg', 'col-lg-4 b-4');

                        // find user video
                        render(tab3, res, 'VideoUrl', '.mp4', 'ratio ratio-16x9 b-4');

                        moveTop.removeClass('hide');
                        footer.addClass('hide');

                        $('html, body').animate({
                            scrollTop: $('#result').offset().top
                        }, 'fast');
                    }
                } catch (error) {
                    console.log(error);
                    socket.close();
                    // location.reload();
                }
            }
        }
    }

    var timer = null;
    number.on("keydown", function(event) {
        if(event.which !== 13) {
            return;
        }

        if (number.val().length !== 11) {
            alert('올바른 전화번호를 입력해주세요');
            return;
        }

        if (timer !== null) {
            clearTimeout(timer);
        }
        timer = setTimeout(function() {
            _post('/api/log', {
                'search': number.val()
            })
        }, 500);

        // let res = _get('/api/cache?phoneNumber=' + number.val());
        // if (res === false) {
        //     alert('유효하지 않은 전화번호입니다');
        //     return;
        // }
        // if (res.hasOwnProperty('responseJSON')) {
        //     alert(res.responseJSON.message);
        //     return;
        // }

        if (socket.readyState !== 1) {
            alert('서버 연결에 실패하였습니다\n새로고침 후 재시도 해주세요');
            // refreshPage(number.val());
            return;
        }else {
            socket.send(number.val());
        }

        searchIcon.addClass('hide');
        spinner.removeClass('hide');
        results.addClass('hide');
        moveTop.addClass('hide');
        limitInfo.addClass('hide');
        // queueInfo.removeClass('hide');
        target.text('');
        targetStatusMessage.text('');
        tab1.empty();
        tab2.empty();
        tab3.empty();
        currentMusic.addClass('hide');
        currentMusicName.text('');
        currentArtistName.text('');
        currentMusicAlbumUrl.attr('src', '');
        previewProfileImg.attr('src', 'img/empty_profile.png');
        footer.removeClass('hide');

        tick = performance.now();
        timeOutTick = 0;

        startCallback = true;
    });

    let received = setInterval(function () {
        if (startCallback) {
            if (socket.readyState === 1) {
                socket.send("Heartbeat");
            }else {
                connect();
            }
        }
    }, 1000);

    connect();

    currentProfile.on("click", function () {
        previewProfile.lightbox('open');
    });

    function replaceDiv() {
        $('#ex-with-icons-tab-1 span').replaceWith(function(){
            return $("<div />", {html: $(this).html()});
        });
        $('#ex-with-icons-tab-2 span').replaceWith(function(){
            return $("<div />", {html: $(this).html()});
        });
        $('#ex-with-icons-tab-3 span').replaceWith(function(){
            return $("<div />", {html: $(this).html()});
        });
    }

    function replaceSpan() {
        $('#ex-with-icons-tab-1 div').replaceWith(function(){
            return $("<span />", {html: $(this).html()});
        });
        $('#ex-with-icons-tab-2 div').replaceWith(function(){
            return $("<span />", {html: $(this).html()});
        });
        $('#ex-with-icons-tab-3 div').replaceWith(function(){
            return $("<span />", {html: $(this).html()});
        });
    }

    if (matchMedia("screen and (min-width: 320px)").matches) {
        replaceSpan();
    } else {
        replaceDiv();
    }
    const media1 = matchMedia("screen and (max-width: 310px)")
    media1.addListener((a) => {
        replaceDiv();
    })
    const media2 = matchMedia("screen and (min-width: 320px)")
    media2.addListener((a) => {
        replaceSpan();
    })

}