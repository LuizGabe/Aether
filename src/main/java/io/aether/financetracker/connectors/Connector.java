package io.aether.financetracker.connectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "connectors")
public class Connector {

    @Id
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    protected Connector() {
    }

    public Connector(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
