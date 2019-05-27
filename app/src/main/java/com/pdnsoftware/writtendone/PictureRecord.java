package com.pdnsoftware.writtendone;

class PictureRecord {
    private int rowId;
    private int scriptId;
    private String picturePath;
    private String pictureName;
    private String createdDate;
    private String thumbnail;

    //Конструктор для выгрузки данных из БД
    PictureRecord(int rowId, int scriptId, String picturePath, String pictureName, String createdDate) {
        this.rowId          = rowId;
        this.scriptId       = scriptId;
        this.picturePath    = picturePath;
        this.pictureName    = pictureName;
        this.createdDate    = createdDate;
        this.thumbnail      = "";
    }

    //Конструктор для выгрузки данных из БД
    PictureRecord(int rowId, int scriptId, String picturePath, String pictureName, String createdDate,
                  String thumbnail) {
        this.rowId          = rowId;
        this.scriptId       = scriptId;
        this.picturePath    = picturePath;
        this.pictureName    = pictureName;
        this.createdDate    = createdDate;
        this.thumbnail      = thumbnail;
    }

    //Набор функций для установки значений
    void setRowId (int val) {rowId = val;}
    void setScriptId (int val) {scriptId = val;}
    void setPicturePath (String val) {picturePath = val;}
    void setPictureName (String val) {pictureName = val;}
    void setCreatedDate (String val) {createdDate = val;}
    void setThumbnail (String val) {thumbnail = val;}

    //Набор функций для чтения значений
    int getRowId() {return rowId;}
    int getScriptId() {return scriptId;}
    String getPicturePath() {return picturePath;}
    String getPictureName() {return pictureName;}
//    String getCreatedDate() {return createdDate;}
    String getThumbnail() {return thumbnail;}
}