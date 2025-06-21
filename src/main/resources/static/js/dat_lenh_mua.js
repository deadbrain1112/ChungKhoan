document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("datLenhForm");
    const xoaBtn = document.getElementById("xoaBtn");
    const nganHangSelect = document.getElementById("nganHangSelect");
    const giaInput = document.getElementById("gia");
    const tongTienMua = document.getElementById("tongTienMua");

    const maCPInput = document.getElementById("maCP");
    const soLuongInput = document.getElementById("soLuong");
    const matKhauInput = document.querySelector("input[name='matKhau']");
    const loaiLenhRadios = document.querySelectorAll("input[name='loaiLenh']");
    const soDuTienSpan = document.getElementById("soDuTien");

    const giaHienThi = document.getElementById("giaHienThi");
    const khongTimThay = document.getElementById("khongTimThay");
    const maCPShow = document.getElementById("maCPShow");
    const giaThamChieu = document.getElementById("giaThamChieu");
    const giaTran = document.getElementById("giaTran");
    const giaSan = document.getElementById("giaSan");

    // Lấy danh sách mã CP viết hoa hợp lệ
    const maCPList = Array.from(document.querySelectorAll("#dsMaCP option")).map(opt => opt.value.trim());

    const updateTongTien = () => {
        const loaiLenh = document.querySelector("input[name='loaiLenh']:checked")?.value;
        const soLuongRaw = soLuongInput.value;
        const giaRaw = giaInput.value;

        const soLuong = parseFloat(parseNumberVN(soLuongRaw)) || 0;
        const gia = parseFloat(parseNumberVN(giaRaw)) || 0;

        if (loaiLenh === 'ATO' || loaiLenh === 'ATC') {
            tongTienMua.textContent = "0 VND";
            return;
        }

        const tong = soLuong * gia;
        tongTienMua.textContent = tong.toLocaleString('vi-VN') + " VND";
    };

    function formatNumberVN(value) {
        const number = value.replace(/\D/g, '');
        return number ? parseInt(number).toLocaleString('vi-VN') : '';
    }

    function parseNumberVN(formatted) {
        return formatted.replace(/\./g, '').replace(/[^0-9]/g, '');
    }

    [giaInput, soLuongInput].forEach(input => {
        input.addEventListener("input", function () {
            const rawValue = parseNumberVN(this.value);
            this.value = formatNumberVN(rawValue);
            updateTongTien();
        });
    });

    form.addEventListener("submit", function () {
        giaInput.value = parseNumberVN(giaInput.value);
        soLuongInput.value = parseNumberVN(soLuongInput.value);
    });

    loaiLenhRadios.forEach(radio => {
        radio.addEventListener("change", function () {
            if (this.value === "ATO" || this.value === "ATC") {
                giaInput.value = "";
                giaInput.readOnly = true;
                giaInput.removeAttribute("name");
            } else {
                giaInput.readOnly = false;
                giaInput.setAttribute("name", "gia");
            }
            updateTongTien();
        });
    });

    xoaBtn?.addEventListener("click", () => {
        const selectedNganHang = nganHangSelect.value;
        form.reset();
        nganHangSelect.value = selectedNganHang;
        maCPInput.value = "";
        soLuongInput.value = "";
        giaInput.value = "";
        matKhauInput.value = "";
        document.querySelector("input[name='loaiLenh'][value='LO']").checked = true;
        giaInput.readOnly = false;
        giaInput.setAttribute("name", "gia");
        tongTienMua.textContent = "0 VND";
        giaHienThi.style.display = "none";
        khongTimThay.style.display = "none";
        maCPShow.textContent = "";
        giaThamChieu.textContent = "";
        giaTran.textContent = "";
        giaSan.textContent = "";
        updateTongTien();
    });

    nganHangSelect.addEventListener("change", function () {
        const selectedMaNH = this.value;
        fetch(`/nhadautu/so-du?nganHang=${encodeURIComponent(selectedMaNH)}`)
            .then(response => response.text())
            .then(data => {
                soDuTienSpan.textContent = data + " VND";
            })
            .catch(error => {
                console.error("Lỗi khi lấy số dư:", error);
                soDuTienSpan.textContent = "0 VND";
            });
    });

    // === Kiểm tra mã CP viết hoa hợp lệ trước khi gửi request ===
    maCPInput.addEventListener("input", function () {
        const maCP = this.value.trim();
		const isValid = maCPList.some(cp => cp === maCP && /^[A-Z]+$/.test(maCP));

        if (!maCP) {
            giaHienThi.style.display = "none";
            khongTimThay.style.display = "none";
            return;
        }

        if (!isValid) {
            giaHienThi.style.display = "none";
            khongTimThay.style.display = "block";
            return;
        }

        // Nếu hợp lệ thì gọi API
        fetch(`/nhadautu/gia-co-phieu?maCP=${encodeURIComponent(maCP)}`)
            .then(response => {
                if (!response.ok) throw new Error();
                return response.json();
            })
            .then(data => {
                maCPShow.textContent = data.maCP;
                giaThamChieu.textContent = data.giaTC;
                giaTran.textContent = data.giaTran;
                giaSan.textContent = data.giaSan;
                giaHienThi.style.display = "block";
                khongTimThay.style.display = "none";
            })
            .catch(() => {
                giaHienThi.style.display = "none";
                khongTimThay.style.display = "block";
            });
    });
});
