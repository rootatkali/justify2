function vote() {
  let values = [
    $("#pts12").val(),
    $("#pts10").val(),
    $("#pts8").val(),
    $("#pts7").val(),
    $("#pts6").val(),
    $("#pts5").val(),
    $("#pts4").val(),
    $("#pts3").val(),
    $("#pts2").val(),
    $("#pts1").val()
  ];

  values = values.map(v => parseInt(v));

  if (new Set(values).size !== values.length) {
    alert('Oi, I told you not to vote for two songs twice!');
    return;
  }

  $.ajax('/api/vote', {
    method: 'POST',
    contentType: 'application/json',
    data: JSON.stringify({
      votes: values
    }),
    success: data => {
      alert(data);
      location.reload();
    },
    error: (jqXHR, textStatus, errorThrown) => {
      alert(errorThrown)
    }
  });
}