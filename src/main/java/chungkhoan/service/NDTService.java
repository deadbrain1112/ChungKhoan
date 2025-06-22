package chungkhoan.service;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.UndoAction;
import chungkhoan.repository.NDTRepository;
import jakarta.persistence.EntityNotFoundException;
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
public class NDTService {

    @Autowired
    private NDTRepository ndtRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Deque<UndoAction> undoStack = new ArrayDeque<>();


    public void themNhaDauTuBangSP(NhaDauTu ndt) {
        jdbcTemplate.execute(
                (Connection conn) -> {
                    CallableStatement cs = conn.prepareCall("{call sp_ThemNhaDauTu(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                    cs.setString(1, ndt.getMaNDT()); // truyền mã NĐT từ đối tượng
                    cs.setString(2, ndt.getHoTen());
                    cs.setDate(3, Date.valueOf(ndt.getNgaySinh()));
                    cs.setString(4, "1"); // Mật khẩu giao dịch mặc định
                    cs.setString(5, ndt.getDiaChi());
                    cs.setString(6, ndt.getPhone());
                    cs.setString(7, ndt.getCmnd());
                    cs.setString(8, ndt.getGioiTinh());
                    cs.setString(9, ndt.getEmail());
                    cs.execute();
                    return null;
                }
        );

        undoStack.push(new UndoAction(
                UndoAction.ActionType.ADD,
                UndoAction.EntityType.NHA_DAU_TU,
                null,
                ndt
        ));
    }


    public void xoaNhaDauTu(String maNDT) {
        NhaDauTu existing = ndtRepository.findById(maNDT).orElse(null);
        if (existing != null) {
            NhaDauTu copy = new NhaDauTu(existing);
            ndtRepository.deleteById(maNDT);
            undoStack.push(new UndoAction(
                    UndoAction.ActionType.DELETE,
                    UndoAction.EntityType.NHA_DAU_TU,
                    copy,
                    null
            ));
        }
    }

    public void capNhatNhaDauTu(String maNDT, NhaDauTu ndtMoi) {
        NhaDauTu ndtCu = ndtRepository.findById(maNDT).orElse(null);
        if (ndtCu != null) {
            NhaDauTu copy = new NhaDauTu(ndtCu);

            ndtCu.setHoTen(ndtMoi.getHoTen());
            ndtCu.setNgaySinh(ndtMoi.getNgaySinh());
            ndtCu.setDiaChi(ndtMoi.getDiaChi());
            ndtCu.setPhone(ndtMoi.getPhone());
            ndtCu.setCmnd(ndtMoi.getCmnd());
            ndtCu.setGioiTinh(ndtMoi.getGioiTinh());
            ndtCu.setEmail(ndtMoi.getEmail());

            ndtCu.setMkGiaoDich(ndtCu.getMkGiaoDich());

            ndtRepository.save(ndtCu);

            undoStack.push(new UndoAction(
                    UndoAction.ActionType.EDIT,
                    UndoAction.EntityType.NHA_DAU_TU,
                    copy,
                    ndtCu
            ));
        } else {
            throw new EntityNotFoundException("Không tìm thấy nhà đầu tư với mã: " + maNDT);
        }
    }


    @Transactional
    public boolean undoThaoTacCuoi() {
        if (undoStack.isEmpty()) return false;

        UndoAction action = undoStack.pop();

        if (action.getEntityType() != UndoAction.EntityType.NHA_DAU_TU) {
            return false;
        }

        NhaDauTu oldNDT = (NhaDauTu) action.getOldData();
        NhaDauTu newNDT = (NhaDauTu) action.getNewData();

        switch (action.getActionType()) {
            case ADD:
                ndtRepository.deleteById(newNDT.getMaNDT());
                break;
            case DELETE:
                ndtRepository.save(oldNDT);
                break;
            case EDIT:
                ndtRepository.save(oldNDT);
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

    public NhaDauTu getNhaDauTuByUsername(String username) {
        NhaDauTu nhaDauTu = ndtRepository.findByUsername(username);
        if (nhaDauTu == null) {
            throw new RuntimeException("Không tìm thấy nhà đầu tư với username: " + username);
        }
        return nhaDauTu;
    }


    // Get paginated list of investors
    public Page<NhaDauTu> getPaginated(int page, int size) {
        if (size == Integer.MAX_VALUE) {
            List<NhaDauTu> allInvestors = ndtRepository.findAll();
            return new PageImpl<>(allInvestors, PageRequest.of(0, Integer.MAX_VALUE), allInvestors.size());
        }
        return ndtRepository.findAll(PageRequest.of(page, size));
    }

    // Search investors by query
    public List<NhaDauTu> searchInvestors(String query) {
        if (query == null || query.trim().isEmpty()) {
            return ndtRepository.findAll();
        }
        // Search by maNDT, hoTen, or cmnd (case-insensitive)
        return ndtRepository.findAll().stream()
                .filter(ndt ->
                        (ndt.getMaNDT() != null && ndt.getMaNDT().toLowerCase().contains(query.toLowerCase())) ||
                                (ndt.getHoTen() != null && ndt.getHoTen().toLowerCase().contains(query.toLowerCase())) ||
                                (ndt.getCmnd() != null && ndt.getCmnd().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Optional<NhaDauTu> findById(String maNDT) {
        return ndtRepository.findById(maNDT);
    }

    public boolean doiMatKhauGiaoDich(String username, String newPassword) {
        NhaDauTu ndt = ndtRepository.findByUsername(username);
        if (ndt != null) {
            ndt.setMkGiaoDich(newPassword);
            ndtRepository.save(ndt);
            return true;
        }
        return false;
    }
    public boolean existsById(String maNDT) {
        return ndtRepository.existsById(maNDT);
    }

    public boolean existsByCmnd(String cmnd) {
        return ndtRepository.existsByCmnd(cmnd);
    }

}