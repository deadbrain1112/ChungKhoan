document.addEventListener("DOMContentLoaded", function () {
    const openButton = document.getElementById("openPasswordPopup");
    if (openButton) {
        openButton.addEventListener("click", function (e) {
            e.preventDefault();
            openPopup();
        });
    }
});

window.openPopup = function () {
    const popup = document.getElementById("changePasswordPopup");
    if (popup) {
        popup.style.display = "flex";
    }
};

window.closePopup = function () {
    const popup = document.getElementById("changePasswordPopup");
    if (popup) {
        popup.style.display = "none";
    }
};
