package chungkhoan.service;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.NhanVien;
import chungkhoan.entity.UndoAction;
import chungkhoan.repository.NhanVienRepository;

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NhanVienService {

    @Autowired
    private NhanVienRepository nhanVienRepository;
    
    private Deque<UndoAction> undoStack = new ArrayDeque<>();

    public NhanVien getNhanVienByUsername(String username) {
        return nhanVienRepository.findByMaNV(username); // username là mã NV
    }
    
    @Transactional
    public boolean undoThaoTacCuoi() {
        if (undoStack.isEmpty()) return false;

        UndoAction action = undoStack.pop();

        switch (action.getActionType()) {
            case ADD:
            	nhanVienRepository.deleteById(action.getNvNewData().getMaNV());
                break;
            case DELETE:
            	nhanVienRepository.save(action.getNvOldData());
                break;
            case EDIT:
            	nhanVienRepository.save(action.getNvOldData());
                break;
        }

        return true;
    }
    
    // Kiểm tra stack rỗng
    public boolean isUndoStackEmpty() {
    	return undoStack.isEmpty();
    }
}
