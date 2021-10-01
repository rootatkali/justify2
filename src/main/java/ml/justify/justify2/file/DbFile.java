package ml.justify.justify2.file;

import ml.justify.justify2.model.Request;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity(name = "files")
public class DbFile {
  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  private String id;
  private String fileName;
  private String fileType;
  @Lob
  private byte[] data;
  private String uploader;

  @ManyToOne
  @JoinColumn(name = "request")
  private Request request;

  public DbFile() {
  }

  public DbFile(String fileName, String fileType, byte[] data, String uploader) {
    this.fileName = fileName;
    this.fileType = fileType;
    this.data = data;
    this.uploader = uploader;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public String getUploader() {
    return uploader;
  }

  public Request getRequest() {
    return request;
  }

  public void setRequest(Request request) {
    this.request = request;
  }
}
