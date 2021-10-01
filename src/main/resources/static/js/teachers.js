function approve(id) {
  $.post(`/api/requests/${id}/approve`, data => location.reload());
}

function reject(id) {
  $.post(`/api/requests/${id}/reject`, data => location.reload());
}

function undo(id) {
  $.post(`/api/requests/${id}/undoApproval`, data => location.reload());
}

function unlock(id) {
  $.post(`/api/requests/${id}/unlock`, data => location.reload());
}

// $(document).ready(() => {
//   $('#table').DataTable({
//     paging: false,
//     searching: false,
//     scrollX: false,
//     info: false,
//   });
// });
