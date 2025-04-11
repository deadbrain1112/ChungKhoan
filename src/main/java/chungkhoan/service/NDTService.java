package chungkhoan.service;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.UndoAction;
import chungkhoan.repository.NDTRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayDeque;
import java.util.Deque;

@Service
public class NDTService {

    @Autowired
    private NDTRepository NDTRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Autowired
    private NDTRepository ndtRepository; // Repository dùng cho Stored Procedure

    private Deque<UndoAction> undoStack = new ArrayDeque<>();

    public void themNhaDauTuBangSP(NhaDauTu ndt) {
        String maNDTMoi = jdbcTemplate.execute(
                (Connection conn) -> {
                    CallableStatement cs = conn.prepareCall("{call sp_ThemNhaDauTu(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                    cs.setString(1, ndt.getHoTen());
                    cs.setDate(2, Date.valueOf(ndt.getNgaySinh()));
                    cs.setString(3, "1"); // MK giao dịch mặc định
                    cs.setString(4, ndt.getDiaChi());
                    cs.setString(5, ndt.getPhone());
                    cs.setString(6, ndt.getCmnd());
                    cs.setString(7, ndt.getGioiTinh());
                    cs.setString(8, ndt.getEmail());
                    cs.registerOutParameter(9, java.sql.Types.NCHAR); // MaNDTMoi
                    cs.execute();
                    return cs.getString(9);
                }
        );

        if (maNDTMoi != null) {
            ndt.setMaNDT(maNDTMoi);
            undoStack.push(new UndoAction(UndoAction.ActionType.ADD, null, ndt));
        }
    }

    public void xoaNhaDauTu(String maNDT) {
        NhaDauTu existing = NDTRepository.findById(maNDT).orElse(null);
        if (existing != null) {
            NDTRepository.deleteById(maNDT);
            undoStack.push(new UndoAction(UndoAction.ActionType.DELETE, existing, null));
        }
    }

    public void capNhatNhaDauTu(String maNDT, NhaDauTu ndtMoi) {
        NhaDauTu ndtCu = NDTRepository.findById(maNDT).orElse(null);
        if (ndtCu != null) {
            ndtMoi.setMkGiaoDich(ndtCu.getMkGiaoDich());

            // Đảm bảo không thay đổi mã NDT
            ndtMoi.setMaNDT(maNDT);

            NDTRepository.save(ndtMoi);
            undoStack.push(new UndoAction(UndoAction.ActionType.EDIT, ndtCu, ndtMoi));
        }
    }

    @Transactional
    public boolean undoThaoTacCuoi() {
        if (undoStack.isEmpty()) return false;

        UndoAction action = undoStack.pop();
        switch (action.getActionType()) {
            case ADD:
                NDTRepository.deleteById(action.getNewData().getMaNDT());
                break;
            case DELETE:
                NDTRepository.save(action.getOldData());
                break;
            case EDIT:
                NDTRepository.save(action.getOldData());
                break;
        }
        return true;
    }
    
    public NhaDauTu getNhaDauTuByUsername(String username) {
        return ndtRepository.findByMaNDT(username);
    }
}
