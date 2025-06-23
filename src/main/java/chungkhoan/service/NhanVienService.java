package chungkhoan.service;

import chungkhoan.entity.NhanVien;
import chungkhoan.entity.UndoAction;
import chungkhoan.repository.NhanVienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Deque<UndoAction> undoStack = new ArrayDeque<>();

    public NhanVien findByMaNV(String maNV) {
        return nhanVienRepository.findById(maNV).orElse(null);
    }

    public void themNhanVienBangSP(NhanVien nv) {
        try {
            jdbcTemplate.execute(
                    (Connection conn) -> {
                        CallableStatement cs = conn.prepareCall("{call sp_ThemNhanVien(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                        cs.setString(1, nv.getMaNV());
                        cs.setString(2, nv.getHoTen());
                        cs.setDate(3, Date.valueOf(nv.getNgaySinh()));
                        cs.setString(4, nv.getDiaChi());
                        cs.setString(5, nv.getPhone());
                        cs.setString(6, nv.getCmnd());
                        cs.setString(7, nv.getGioiTinh());
                        cs.setString(8, nv.getEmail());
                        cs.registerOutParameter(9, java.sql.Types.NCHAR); // Không cần sử dụng giá trị trả về
                        cs.execute();
                        return null;
                    }
            );

            undoStack.push(new UndoAction(
                    UndoAction.ActionType.ADD,
                    UndoAction.EntityType.NHAN_VIEN,
                    null,
                    nv
            ));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Mã nhân viên đã tồn tại")) {
                throw new RuntimeException("Mã nhân viên đã tồn tại!");
            } else if (errorMessage.contains("CMND đã tồn tại")) {
                throw new RuntimeException("CMND đã tồn tại!");
            } else {
                throw new RuntimeException("Lỗi khi thêm nhân viên: " + errorMessage);
            }
        }
    }

    public void xoaNhanVien(String maNV) {
        NhanVien existing = nhanVienRepository.findById(maNV).orElse(null);
        if (existing != null) {
            NhanVien copy = new NhanVien(existing);
            nhanVienRepository.deleteById(maNV);
            undoStack.push(new UndoAction(
                    UndoAction.ActionType.DELETE,
                    UndoAction.EntityType.NHAN_VIEN,
                    copy,
                    null
            ));
        }
    }

    public void capNhatNhanVien(String maNV, NhanVien nvMoi) {
        NhanVien nvCu = nhanVienRepository.findById(maNV).orElse(null);
        if (nvCu != null) {
            NhanVien copy = new NhanVien(nvCu);

            nvMoi.setMaNV(maNV);

            nhanVienRepository.save(nvMoi);
            undoStack.push(new UndoAction(
                    UndoAction.ActionType.EDIT,
                    UndoAction.EntityType.NHAN_VIEN,
                    copy,
                    nvMoi
            ));
        }
    }

    @Transactional
    public boolean undoThaoTacCuoi() {
        if (undoStack.isEmpty()) return false;

        UndoAction action = undoStack.pop();

        if (action.getEntityType() != UndoAction.EntityType.NHAN_VIEN) {
            return false;
        }

        NhanVien oldNV = (NhanVien) action.getOldData();
        NhanVien newNV = (NhanVien) action.getNewData();

        switch (action.getActionType()) {
            case ADD:
                nhanVienRepository.deleteById(newNV.getMaNV());
                break;
            case DELETE:
                nhanVienRepository.save(oldNV);
                break;
            case EDIT:
                nhanVienRepository.save(oldNV);
                break;
        }

        return true;
    }

    public boolean isUndoStackEmpty() {
        return undoStack.isEmpty();
    }

    public void clearUndoStack() {
        undoStack.clear();
    }

    public NhanVien getNhanVienByUsername(String username) {
        NhanVien nhanVien = nhanVienRepository.findByUsername(username);
        if (nhanVien == null) {
            throw new RuntimeException("Không tìm thấy nhân viên với username: " + username);
        }
        return nhanVien;
    }

    // Get paginated list of employees
    public Page<NhanVien> getPaginated(int page, int size) {
        if (size == Integer.MAX_VALUE) {
            List<NhanVien> allEmployees = nhanVienRepository.findAll();
            return new PageImpl<>(allEmployees, PageRequest.of(0, Integer.MAX_VALUE), allEmployees.size());
        }
        return nhanVienRepository.findAll(PageRequest.of(page, size));
    }

    // Search employees by query
    public List<NhanVien> searchEmployees(String query) {
        if (query == null || query.trim().isEmpty()) {
            return nhanVienRepository.findAll();
        }
        // Search by maNV, hoTen, or cmnd 
        return nhanVienRepository.findAll().stream()
                .filter(nv ->
                        (nv.getMaNV() != null && nv.getMaNV().toLowerCase().contains(query.toLowerCase())) ||
                                (nv.getHoTen() != null && nv.getHoTen().toLowerCase().contains(query.toLowerCase())) ||
                                (nv.getCmnd() != null && nv.getCmnd().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Optional<NhanVien> findById(String maNV) {
        return nhanVienRepository.findById(maNV);
    }

    // Kiểm tra xem MaNV đã tồn tại chưa
    public boolean existsById(String maNV) {
        return nhanVienRepository.existsById(maNV);
    }

    // Kiểm tra xem CMND đã tồn tại chưa
    public boolean existsByCmnd(String cmnd) {
        return nhanVienRepository.existsByCmnd(cmnd);
    }


}