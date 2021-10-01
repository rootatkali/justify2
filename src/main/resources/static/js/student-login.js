function onClickLogin() {
    let username = $('#user').val();
    let password = $('#pass').val();
    $.ajax("/api/students/login", {
        method: 'POST',
        data: {
            username: username,
            password: password
        },
        success: function (data, status, xhr) {
            window.location.href = "/";
        },
        error: function (jqXhr, textStatus, errorMessage) {
            alert("Invalid login. Please try again.");
        }
    });
}

// document.getElementById("login").onclick = onClickLogin;

$('#login-form').submit(e => {
    e.preventDefault();
    onClickLogin();
});
