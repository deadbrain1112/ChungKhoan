package chungkhoan.service;

import chungkhoan.entity.NganHang;
import chungkhoan.entity.UndoAction;
import chungkhoan.repository.NganHangRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NganHangService {

    @Autowired
    private NganHangRepository nganHangRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Deque<UndoAction> undoStack = new ArrayDeque<>();

    public NganHang findByMaNH(String maNH) {
        return nganHangRepository.findById(maNH).orElse(null);
    }

    public void themNganHang(NganHang nh) {
        try {
            jdbcTemplate.execute(
                    (Connection conn) -> {
                        CallableStatement cs = conn.prepareCall("{call sp_ThemNganHang(?, ?, ?, ?, ?)}");
                        cs.setString(1, nh.getMaNH());
                        cs.setString(2, nh.getTenNH());
                        cs.setString(3, nh.getDiaChi());
                        cs.setString(4, nh.getPhone());
                        cs.setString(5, nh.getEmail());
                        cs.execute();
                        return null;
                    }
            );

            undoStack.push(new UndoAction(
                    UndoAction.ActionType.ADD,
                    UndoAction.EntityType.NGAN_HANG,
                    null,
                    nh
            ));
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("Mã ngân hàng đã tồn tại")) {
                throw new RuntimeException("Mã ngân hàng đã tồn tại!");
            } else {
                throw new RuntimeException("Lỗi khi thêm ngân hàng: " + errorMessage);
            }
        }
    }

    public void xoaNganHang(String maNH) {
        NganHang existing = nganHangRepository.findById(maNH).orElse(null);
        if (existing != null) {
            NganHang copy = new NganHang(existing);
            nganHangRepository.deleteById(maNH);
            undoStack.push(new UndoAction(
                    UndoAction.ActionType.DELETE,
                    UndoAction.EntityType.NGAN_HANG,
                    copy,
                    null
            ));
        }
    }

    public void capNhatNganHang(String maNH, NganHang nhMoi) {
        NganHang nhCu = nganHangRepository.findById(maNH).orElse(null);
        if (nhCu != null) {
            NganHang copy = new NganHang(nhCu);

            nhMoi.setMaNH(maNH);
            nganHangRepository.save(nhMoi);
            undoStack.push(new UndoAction(
                    UndoAction.ActionType.EDIT,
                    UndoAction.EntityType.NGAN_HANG,
                    copy,
                    nhMoi
            ));
        }
    }

    @Transactional
    public boolean undoThaoTacCuoi() {
        if (undoStack.isEmpty()) return false;

        UndoAction action = undoStack.pop();

        if (action.getEntityType() != UndoAction.EntityType.NGAN_HANG) {
            return false;
        }

        NganHang oldNH = (NganHang) action.getOldData();
        NganHang newNH = (NganHang) action.getNewData();

        switch (action.getActionType()) {
            case ADD:
                nganHangRepository.deleteById(newNH.getMaNH());
                break;
            case DELETE:
                nganHangRepository.save(oldNH);
                break;
            case EDIT:
                nganHangRepository.save(oldNH);
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

    public Page<NganHang> getPaginated(int page, int size) {
        if (size == Integer.MAX_VALUE) {
            List<NganHang> allBanks = nganHangRepository.findAll();
            return new PageImpl<>(allBanks, PageRequest.of(0, Integer.MAX_VALUE), allBanks.size());
        }
        return nganHangRepository.findAll(PageRequest.of(page, size));
    }

    public List<NganHang> searchBanks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return nganHangRepository.findAll();
        }
        return nganHangRepository.findAll().stream()
                .filter(nh ->
                        (nh.getMaNH() != null && nh.getMaNH().toLowerCase().contains(query.toLowerCase())) ||
                                (nh.getTenNH() != null && nh.getTenNH().toLowerCase().contains(query.toLowerCase())) ||
                                (nh.getDiaChi() != null && nh.getDiaChi().toLowerCase().contains(query.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Optional<NganHang> findById(String maNH) {
        return nganHangRepository.findById(maNH);
    }

    public boolean existsById(String maNH) {
        return nganHangRepository.existsById(maNH);
    }

    public List<NganHang> findAll() {
        return nganHangRepository.findAll();
    }
    
    public Optional<NganHang> findByMaNHOpt(String maNH) {
        return nganHangRepository.findById(maNH);
    }
    
    public boolean capNhatNganHang(String maNH, String tenNH, String diaChi, String phone, String email) {
        Optional<NganHang> optional = nganHangRepository.findByMaNH(maNH);

        if (optional.isPresent()) {
            NganHang nh = optional.get();
            nh.setTenNH(tenNH);
            nh.setDiaChi(diaChi);
            nh.setPhone(phone);
            nh.setEmail(email);
            nganHangRepository.save(nh);
            return true;
        } else {
            return false;
        }
    }

}