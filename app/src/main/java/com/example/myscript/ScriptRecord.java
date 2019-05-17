package com.example.myscript;

public class ScriptRecord {

    private int row_id;
    private String script_title;
    private String script_content;
    private String created_date;
    private int finished;
    private String finish_date;

    ScriptRecord(int row_id, String title, String content) {
        this.row_id         = row_id;
        this.script_title   = title;
        this.script_content = content;
        this.created_date   = MyScriptDB.DEFAULT_DATE_STRING;
        this.finished       = MyScriptDB.MARK_AS_OPENED;
        this.finish_date    = "";
    }

    ScriptRecord(int row_id, String title, String content, String created_date, int finished, String finish_date) {
        this.row_id         = row_id;
        this.script_title   = title;
        this.script_content = content;
        this.created_date   = created_date;
        this.finished       = finished;
        this.finish_date    = finish_date;
    }

    int getRowId() {return row_id;}
    String getTitle() {return script_title;}
    public String getContent() {return script_content;}
    String getCreatedDate() {return created_date;}
    public int getFinished() {return finished;}
    public String getFinishDate() {return finish_date;}

    public void setRowId(int val) {row_id = val;}
    public void setTitle(String val) {script_title = val;}
    public void setContent(String val) {script_content = val;}
    public void setCreatedDate(String val) {created_date = val;}
    public void setFinished(int statusVal, String dateVal) {finished = statusVal; finish_date = dateVal;}
}
