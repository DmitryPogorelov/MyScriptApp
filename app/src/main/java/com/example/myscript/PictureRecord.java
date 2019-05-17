package com.example.myscript;

public class PictureRecord {
    private int rowId;
    private int scriptId;
    private String picturePath;
    private String pictureName;
    private String createdDate;

    //Конструктор для создания пустой записи
    public PictureRecord() {
        this.rowId          = -1;
        this.scriptId       = -1;
        this.picturePath    = "";
        this.pictureName    = "";
        this.createdDate    = "";
    }

    //Конструктор для создания новой записи - у него нет row_id
    public PictureRecord(int scriptId, String picturePath, String pictureName, String createdDate) {
        this.rowId          = -1;
        this.scriptId       = scriptId;
        this.picturePath    = picturePath;
        this.pictureName    = pictureName;
        this.createdDate    = createdDate;
    }
    //Конструктор для выгрузки данных из БД
    PictureRecord(int rowId, int scriptId, String picturePath, String pictureName, String createdDate) {
        this.rowId          = rowId;
        this.scriptId       = scriptId;
        this.picturePath    = picturePath;
        this.pictureName    = pictureName;
        this.createdDate    = createdDate;
    }

    //Набор функций для установки значений
    void setRowId (int val) {rowId = val;}
    void setScriptId (int val) {scriptId = val;}
    void setPicturePath (String val) {picturePath = val;}
    void setPictureName (String val) {pictureName = val;}
    void setCreatedDate (String val) {createdDate = val;}

    //Набор функций для чтения значений
    int getRowId() {return rowId;}
    int getScriptId() {return scriptId;}
    String getPicturePath() {return picturePath;}
    String getPictureName() {return pictureName;}
    String getCreatedDate() {return createdDate;}
}
