package com.example.examenentrevistas.Model;

import java.util.Date;

public class Entrevista {
    private int idOrden;
    private String descripcion;
    private String periodista;
    private Date fecha;
    private String imagenUrl;
    private String audioUrl;

    public Entrevista() {

    }

    public Entrevista(int idOrden, String descripcion, String periodista, Date fecha, String imagenUrl, String audioUrl) {
        this.idOrden = idOrden;
        this.descripcion = descripcion;
        this.periodista = periodista;
        this.fecha = fecha;
        this.imagenUrl = imagenUrl;
        this.audioUrl = audioUrl;
    }

    public Entrevista(String descripcion, String periodista, Date fecha, String imagenUrl, String audioUrl) {
        this.descripcion = descripcion;
        this.periodista = periodista;
        this.fecha = fecha;
        this.imagenUrl = imagenUrl;
        this.audioUrl = audioUrl;
    }

    public Entrevista(String periodista, Date fecha) {
        this.periodista = periodista;
        this.fecha = fecha;
    }

    public int getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(int idOrden) {
        this.idOrden = idOrden;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPeriodista() {
        return periodista;
    }

    public void setPeriodista(String periodista) {
        this.periodista = periodista;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
}
