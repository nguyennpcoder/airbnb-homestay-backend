package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tin_nhan")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TinNhan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tin_nhan")
    private Long maTinNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nguoi_gui", nullable = false)
    private NguoiDung nguoiGui;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nguoi_nhan", nullable = false)
    private NguoiDung nguoiNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dat_cho")
    private DatCho datCho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong")
    private Phong phong;

    @Column(name = "noi_dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "da_doc")
    private Boolean daDoc = false;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ket_noi_duoc_chap_nhan")
    private Boolean ketNoiDuocChapNhan = false;

    @Column(name = "file_type", length = 255)
    private String fileType;

    @Column(name = "file_url", length = 255)
    private String fileUrl;

    @Column(name = "reactions", length = 255)
    private String reactions;

    @Column(name = "is_auto_reply")
    private Boolean autoReply = false;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}
