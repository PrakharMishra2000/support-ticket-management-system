document.addEventListener("click", (event) => {
  const dismissButton = event.target.closest("[data-dismiss-alert]");
  if (!dismissButton) {
    return;
  }

  const alert = dismissButton.closest('[role="alert"]');
  if (alert) {
    alert.remove();
  }
});
