package nhatroxanh.com.Nhatroxanh.Model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "ElectricWaterReading")
public class ElectricWaterReading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer roomId;

    @Column(name = "electric_old")
    private Float electricOld;

    @Column(name = "electric_new")
    private Float electricNew;

    @Column(name = "water_old")
    private Float waterOld;

    @Column(name = "water_new")
    private Float waterNew;

    @Column(name = "electric_price")
    private Float electricPrice;

    @Column(name = "water_price")
    private Float waterPrice;

    @Column(name = "wifi_price")
    private Float wifiPrice;
}
