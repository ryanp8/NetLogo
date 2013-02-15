// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.modelingcommons;

import org.apache.http.Consts;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;

import java.io.UnsupportedEncodingException;

public abstract class MultiPartPostRequest extends Request {

  private MultipartEntity entity;

  public MultiPartPostRequest(HttpClient http, String url) {
    super(http, url);
    entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
  }

  public void addStringParam(String name, String value) {
    try {
      entity.addPart(name, new StringBody(value, "text/plain", Consts.UTF_8));
    } catch (UnsupportedEncodingException e) {
      //UTF8 is required to be supported by Java
      //This will never be thrown
    }
  }

  public void addFileParam(String name, String fileContents, final String fileName) {
    try {
      entity.addPart(name, new StringBody(fileContents, "text/plain", Consts.UTF_8) {

        public String getFilename() {
          return fileName;
        }

      });
    } catch (UnsupportedEncodingException e) {
      //UTF8 is required to be supported by Java
      //This will never be thrown
    }
  }

  public void addFileParam(String name, byte[] fileContents, final String fileName) {
    entity.addPart(name, new ByteArrayBody(fileContents, fileName));
  }

  @Override
  public void execute() {
    request.setEntity(entity);
    super.execute();
  }

}
