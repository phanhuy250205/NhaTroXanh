package nhatroxanh.com.Nhatroxanh.Model.enity;

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
    private int roomId;

    @Column(name = "electric_old")
    private float electricOld;

    @Column(name = "electric_new")
    private float electricNew;

    @Column(name = "water_old")
    private float waterOld;

    @Column(name = "water_new")
    private float waterNew;

    @Column(name = "electric_price")
    private float electricPrice;

    @Column(name = "water_price")
    private float waterPrice;

    @Column(name = "wifi_price")
    private float wifiPrice;
}
