let me;

$.get("/api/students/me", user => me = user);

const modal = document.getElementById("request-modal");
modal.addEventListener('show.bs.modal', evt => {
  const btn = evt.relatedTarget;
  // if from behaves list - load data to modal
  if (btn.hasAttribute('data-bs-behave')) {
    const behave = JSON.parse(btn.getAttribute('data-bs-behave'));
    let fullDay = btn.getAttribute('data-bs-full') === 'true';

    document.getElementById('date-start').value = behave.lessonDate.substr(0, 10);
    document.getElementById('period-start').value = fullDay ? 0 : behave.lesson;
    document.getElementById('date-end').value = behave.lessonDate.substr(0, 10);
    document.getElementById('period-end').value = fullDay ? 15 : behave.lesson;
    document.getElementById('event').value = behave.achvaCode;
  }

  document.getElementById('sendRequest').onclick = () => {
    $.ajax('/api/requests', {
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify({
        userId: me.id,
        dateStart: $("#date-start").val(),
        dateEnd: $("#date-end").val(),
        periodStart: parseInt($("#period-start").val()),
        periodEnd: parseInt($("#period-end").val()),
        eventCode: parseInt($("#event").val()),
        justificationCode: parseInt($("#approval").val()),
        note: $("#note").val()
      }),
      success: request => {
        let fileElement = document.getElementById('files');
        let files = fileElement.files;
        if (files.length > 0) {
          let formData = new FormData();
          for (let i = 0; i < files.length; i++) {
            formData.append('files', files[i]);
          }
          $.ajax(`/api/files/request/${request.requestId}/multiple`, {
            method: 'POST',
            data: formData,
            processData: false,
            contentType: false,
            success: data => location.reload(),
            error: (jqXHR, textStatus, errorThrown) => alert(errorThrown)
          });
        } else location.reload();
      },
      error: (jqXHR, textStatus, errorThrown) => {
        alert(errorThrown)
      }
    });
  };
});

function logout() {
  $.post(`/api/students/logout`, data => location.href = "/");
}

$('#btnLogout').click(logout);
