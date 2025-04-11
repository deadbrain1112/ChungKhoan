package chungkhoan.service;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.UndoAction;
import chungkhoan.repository.NDTRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.ArrayDeque;
import java.util.Deque;

@Service
public class NDTService {

    @Autowired
    private NDTRepository NDTRepository;

    @Autowired
    private NDTRepository ndtRepository; // Repository dùng cho Stored Procedure

    private Deque<UndoAction> undoStack = new ArrayDeque<>();

    public void themNhaDauTu(NhaDauTu ndt) {
        NDTRepository.save(ndt);
        undoStack.push(new UndoAction(UndoAction.ActionType.ADD, null, ndt));
    }

    public void themNhaDauTuBangSP(NhaDauTu ndt) {
        ndtRepository.themNhaDauTu(
                ndt.getHoTen(),
                Date.valueOf(ndt.getNgaySinh()),
                "1", // Giả định mã người dùng tạo mặc định
                ndt.getDiaChi(),
                ndt.getPhone(),
                ndt.getCmnd(),
                ndt.getGioiTinh(),
                ndt.getEmail()
        );
        // Không undo được do không lấy được MaNDT trả về từ Stored Procedure
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
            NDTRepository.save(ndtMoi);
            undoStack.push(new UndoAction(UndoAction.ActionType.EDIT, ndtCu, ndtMoi));
        }
    }

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
