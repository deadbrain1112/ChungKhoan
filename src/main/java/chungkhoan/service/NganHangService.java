package chungkhoan.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import chungkhoan.entity.NganHang;
import chungkhoan.repository.NganHangRepository;

@Service
public class NganHangService {
	
	@Autowired
    private NganHangRepository nganHangRepository;
	
	public List<NganHang> getAll() {
        return nganHangRepository.findAll();
    }
}
