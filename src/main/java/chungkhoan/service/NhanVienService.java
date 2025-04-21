package chungkhoan.service;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.NhanVien;
import chungkhoan.entity.UndoAction;
import chungkhoan.repository.NhanVienRepository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final Deque<UndoAction> undoStack = new ArrayDeque<>();

    public NhanVien getNhanVienByUsername(String username) {
        return nhanVienRepository.findByMaNV(username);
    }
    
    public void themNhanVienBangSP(NhanVien nv) {
        String maNVMoi = jdbcTemplate.execute(
                (Connection conn) -> {
                    CallableStatement cs = conn.prepareCall("{call sp_ThemNhanVien(?, ?, ?, ?, ?, ?, ?, ?, ?)}");
                    cs.setString(1, nv.getHoTen());
                    cs.setDate(2, Date.valueOf(nv.getNgaySinh()));
                    cs.setString(3, "1");
                    cs.setString(4, nv.getDiaChi());
                    cs.setString(5, nv.getPhone());
                    cs.setString(6, nv.getCmnd());
                    cs.setString(7, nv.getGioiTinh());
                    cs.setString(8, nv.getEmail());
                    cs.registerOutParameter(9, java.sql.Types.NCHAR);
                    cs.execute();
                    return cs.getString(9);
                }
        );

        if (maNVMoi != null) {
        	nv.setMaNV(maNVMoi);
            undoStack.push(new UndoAction(
                    UndoAction.ActionType.ADD,
                    UndoAction.EntityType.NHAN_VIEN,
                    null,
                    nv
            ));
        }
    }
    
    public void xoaNhanVien(String maNV) {
        NhanVien nv = nhanVienRepository.findById(maNV).orElse(null);
        if (nv != null) {
            nhanVienRepository.deleteById(maNV);
            undoStack.push(new UndoAction(
                UndoAction.ActionType.DELETE,
                UndoAction.EntityType.NHAN_VIEN,
                nv,  // oldData: trước khi xóa
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
        if (action.getEntityType() != UndoAction.EntityType.NHAN_VIEN) return false;

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


    // Kiểm tra stack rỗng
    public boolean isUndoStackEmpty() {
    	return undoStack.isEmpty();
    }
    
    public void clearUndoStack() {
        undoStack.clear();
    }
}