package io.dcloud.js.file;

import io.dcloud.common.adapter.io.DHFile;

import java.io.File;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Description:js的file扩展</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-1-17 上午9:19:33 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-17 上午9:19:33</pre>
 */
public class JsFile {

	/**
	 * 
	 * Description:获得JS的file对象
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-17 上午10:18:21</pre>
	 */
	protected static JSONObject getJsFile(String pType, long lastModifyDate,long pSize, String pName, String pFullPath){
		JSONObject _json = new JSONObject();
		try {
			_json.put("lastModifiedDate", lastModifyDate);
			_json.put("type", pType);//type值为number类型
			_json.put("size", pSize);
			_json.put("name", pName);
			_json.put("fullPath", pFullPath);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return _json;
	}
	
	/**
	 * 
	 * Description:获得JS的fileSystem对象
	 * @param pName
	 * @param pRoot
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-17 上午11:36:05</pre>
	 */
	protected static JSONObject getJsFileSystem(String pFsName, int pType, String pName, String pRoot, String pRemoteURL){
		JSONObject _json = new JSONObject();
		try {
			_json.put("name", pFsName);
			_json.put("type", pType);
			JSONObject root = new JSONObject();
			{
				root.put("name", pName);
				root.put("fullPath", pRoot);
				root.put("remoteURL", pRemoteURL);
			}
			_json.put("root", root);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return _json;
	}
	/**
	 * 
	 * Description:获得JS的FileEntry对象
	 * @param pName
	 * @param pFullPath
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-22 下午4:24:11</pre>
	 */
	protected static JSONObject getJsFileEntry(String pName, String pFullPath, String pRemoteURL,boolean isDir){
		JSONObject _json = new JSONObject();
		try {
			_json.put("isDirectory", isDir);
			_json.put("isFile", !isDir);
			_json.put("name", pName);
			_json.put("remoteURL", pRemoteURL);
			_json.put("fullPath", pFullPath);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return _json;
	}
	
	protected static JSONObject getJsFileEntryAndFS(String pName, String pFullPath, boolean isDir,
			String pRemoteURL,String pFsName,int pType, JSONObject pRoot){
		JSONObject _json = new JSONObject();
		try {
			_json.put("isDirectory", isDir);
			_json.put("isFile", !isDir);
			_json.put("name", pName);
			_json.put("remoteURL", pRemoteURL);
			_json.put("fullPath", pFullPath);
			_json.put("fsName", pFsName);
			_json.put("type", pType);
			_json.put("fsRoot", pRoot);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return _json;
	}
	/**
	 * 
	 * Description:根据文件路径返回readEntriesJSON数据
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-22 下午4:52:23</pre>
	 */
	protected static JSONArray readEntries(String pPath, String pRemoteURL){
		JSONArray ret = new JSONArray();
		pPath += pPath.endsWith(File.separator) ? "" : File.separator;
		pRemoteURL += pRemoteURL.endsWith(File.separator) ? "" : File.separator;
		File _fileEntries = new File(pPath);
		String[] _files = _fileEntries.list();
		File _file;
		String _filePath;
		if(_files != null){
			for(int i=0;i<_files.length;i++){
				_filePath = pPath + _files[i];
				_file = new File(_filePath);
				JSONObject _result = new JSONObject();
				boolean isDir = _file.isDirectory();
				String name = _files[i] ;
				String remoteURL = pRemoteURL + name;
				String fullPath = _filePath ;
				try {
					_result.put("isDirectory", isDir);
					_result.put("isFile", !isDir);
					_result.put("name", name);
					_result.put("remoteURL", remoteURL);
					_result.put("fullPath", fullPath);
					ret.put(_result);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return ret;
	}
	/**
	 * Description:获取文件信息
	 * @param pFullPath
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-17 下午4:16:49</pre>
	 */
	public static JSONObject getFileMetadata(String pFullPath, String pType) {
		File _file = new File(pFullPath);
		return getJsFile(pType, _file.lastModified(), DHFile.getFileSize(_file), _file.getName(), pFullPath);
	}
	/**
	 * Description:
	 * @param pFullPath 要获取文件或目录的路径
	 * @param recursive 是否递归
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-28 上午5:07:17</pre>
	 */
	public static JSONObject getMetadata(String pFullPath,boolean recursive) {
		File _file = new File(pFullPath);
		if(!_file.exists()){
			return null;
		}
		Metadata metadata = new Metadata();
		metadata.lastModified = _file.lastModified();
		if(_file.isDirectory()){
			File[] _files = _file.listFiles();
			if(_files != null){
				for(File file : _files){//遍历文件 和 目录
					getDirSize(file, metadata, recursive);
				}
			}
		}else{
			metadata.size = _file.length();
		}
		return metadata.toJSONObject();
	}
	
	static class Metadata{
		static final String METADATA_TEMPLATE = "{"+
		"lastModifiedDate : %d," +
		"size : %d,"+
		"directoryCount : %d,"+
		"fileCount : %d"+
        "}";
		long lastModified;
		long size ;
		int directoryCount = 0;
		int fileCount = 0;
		
		public JSONObject toJSONObject(){
			String _json = String.format(Locale.ENGLISH,METADATA_TEMPLATE, lastModified,size,directoryCount,fileCount);
			try {
				return new JSONObject(_json);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return new JSONObject();
		}
	}

	public static void getDirSize(File file,Metadata metadata,boolean recursive){
		if(file.isDirectory()){//目录时
			if(recursive){
				File[] fileList = file.listFiles();
				if(fileList != null){
					for(File _file : fileList){
						getDirSize(_file,metadata,recursive);
					}
				}
			}
			metadata.directoryCount++;
		}else{
			metadata.size += file.length();
			metadata.fileCount++;
		}
	}
}
