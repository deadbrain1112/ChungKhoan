document.addEventListener("DOMContentLoaded", function () {

    function trimInputsOnSubmit(formSelector) {
        const form = document.querySelector(formSelector);
        if (!form) return;

        form.addEventListener("submit", function () {
            const fields = form.querySelectorAll("input[type='text'], input[type='email'], input[type='password']");
            fields.forEach(input => {
                if (input.value) input.value = input.value.trim();
            });
        });
    }

    // Áp dụng cho các form chính
    trimInputsOnSubmit("#add-form form");
    trimInputsOnSubmit("#edit-form form");
    trimInputsOnSubmit("#bank-subform-2-form");

    // Với các form thêm dòng subform động: xử lý cả input vừa tạo
    document.addEventListener("submit", function (e) {
        const dynamicFields = e.target.querySelectorAll("input[type='text'], input[type='email'], input[type='password']");
        dynamicFields.forEach(input => {
            if (input.value) input.value = input.value.trim();
        });
    });
});

