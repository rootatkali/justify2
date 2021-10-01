function deleteFile(id) {
  $.ajax(`/api/files/${id}`, {
    method: 'DELETE',
    success: data => location.reload(),
    error: (jqXHR, textStatus, errorThrown) => alert(errorThrown)
  });
}
