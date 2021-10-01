function cancelRequest(id) {
  $.ajax(`/api/requests/${id}/cancel`, {
    method: "POST",
    success: data => location.reload(),
    error: (jqXHR, textStatus, errorThrown) => alert(errorThrown)
  });
}

const editModal = document.getElementById('edit-modal');
editModal.addEventListener('show.bs.modal', event => {
  const btn = event.relatedTarget;
  const id = btn.getAttribute('data-bs-id');
  $.get(`/api/requests/${id}`, data => {
    $('#editModalTitle').text(`Editing request ${id}`);
    document.getElementById('edit-date-start').value = data.dateStart;
    document.getElementById('edit-period-start').value = data.periodStart;
    document.getElementById('edit-date-end').value = data.dateEnd;
    document.getElementById('edit-period-end').value = data.periodEnd;
    document.getElementById('edit-event').value = data.eventCode;
    document.getElementById('edit-approval').value = data.justificationCode;
    document.getElementById('edit-note').value = data.note
      .replaceAll('&amp;', '&')
      .replaceAll('&lt;', '<')
      .replaceAll('&gt;', '>')
      .replaceAll('&quot;', '"')
      .replaceAll('&#x27;', "'");
    document.getElementById('edit-sendRequest').onclick = () => {
      $.ajax(`/api/requests/${id}`, {
        method: 'PATCH',
        contentType: 'application/json',
        data: JSON.stringify({
          userId: me.id,
          dateStart: $('#edit-date-start').val(),
          dateEnd: $('#edit-date-end').val(),
          periodStart: parseInt($('#edit-period-start').val()),
          periodEnd: parseInt($('#edit-period-end').val()),
          eventCode: parseInt($('#edit-event').val()),
          justificationCode: parseInt($('#edit-approval').val()),
          note: $('#edit-note').val()
        }),
        success: request => {
          let eFileElement = document.getElementById('edit-files');
          let eFiles = eFileElement.files;
          if (eFiles.length > 0) {
            let formData = new FormData();
            for (let i = 0; i < eFiles.length; i++) {
              formData.append('files', eFiles[i]);
            }
            $.ajax(`/api/files/request/${id}/multiple`, {
              method: 'POST',
              data: formData,
              processData: false,
              contentType: false,
              success: data => location.reload(),
              error: (jqXHR, textStatus, errorThrown) => alert(errorThrown)
            });
          } else location.reload();
        },
        error: (jqXHR, textStatus, errorThrown) => alert(errorThrown)
      });
    };
  });
});