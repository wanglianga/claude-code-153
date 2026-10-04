package com.community.waste.entity;

import jakarta.persistence.*;

/** 司机 */
@Entity
@Table(name = "driver")
public class Driver extends BaseEntity {
    @Column(nullable = false, length = 32)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 32)
    private String licenseNo;

    @Column(length = 64)
    private String companyName;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLicenseNo() { return licenseNo; }
    public void setLicenseNo(String licenseNo) { this.licenseNo = licenseNo; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}
