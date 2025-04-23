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
    private NDTRepository ndtRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Deque<UndoAction> undoStack = new ArrayDeque<>();
    
    public NhaDauTu findByMaNDT(String maNDT) {
    	return ndtRepository.findByMaNDT(maNDT);
    }

    public void themNhaDauTuBangSP(NhaDauTu ndt) {
        String maNDTMoi = jdbcTemplate.execute(
                (Connection conn) -> {
                    CallableStatement cs = conn.prepareCall("{call sp_ThemNhaDauTu(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                    cs.setString(1, ndt.getHoTen());
                    cs.setDate(2, Date.valueOf(ndt.getNgaySinh()));
                    cs.setString(3, "1");
                    cs.setString(4, ndt.getDiaChi());
                    cs.setString(5, ndt.getPhone());
                    cs.setString(6, ndt.getCmnd());
                    cs.setString(7, ndt.getGioiTinh());
                    cs.setString(8, ndt.getEmail());
                    cs.registerOutParameter(9, java.sql.Types.NCHAR);
                    cs.execute();
                    return cs.getString(9);
                }
        );

        if (maNDTMoi != null) {
            ndt.setMaNDT(maNDTMoi);
            undoStack.push(new UndoAction(
                    UndoAction.ActionType.ADD,
                    UndoAction.EntityType.NHA_DAU_TU,
                    null,
                    ndt
            ));
        }
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

            ndtMoi.setMaNDT(maNDT);
            ndtMoi.setMkGiaoDich(ndtCu.getMkGiaoDich());

            ndtRepository.save(ndtMoi);
            undoStack.push(new UndoAction(
                    UndoAction.ActionType.EDIT,
                    UndoAction.EntityType.NHA_DAU_TU,
                    copy,
                    ndtMoi
            ));
        }
    }

    @Transactional
    public boolean undoThaoTacCuoi() {
        if (undoStack.isEmpty()) return false;

        UndoAction action = undoStack.pop();

        if (action.getEntityType() != UndoAction.EntityType.NHA_DAU_TU) {
            return false; // chưa hỗ trợ undo cho entity khác
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
}