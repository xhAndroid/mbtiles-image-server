package com.wclwksn.mbtileimageserver.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.wclwksn.mbtileimageserver.MblistBean;
import com.wclwksn.mbtileimageserver.MbtileHandle.MBTilesReader;
import com.wclwksn.mbtileimageserver.MbtileHandle.MetadataEntry;
import com.wclwksn.mbtileimageserver.model.*;

@RestController
@RequestMapping("/mbtiles")
public class MbtileController {

	private static final Logger log = LoggerFactory.getLogger(MbtileController.class);
	@Autowired
	MblistBean _mbtileBean;

	@Value("${mbdata.path}")
	private String mbpath;

	@RequestMapping(method = RequestMethod.GET, path = "datapath", produces = { "application/json;charset=UTF-8" })
	public String DataPath() {

		return String.format("{\"path\": \"%s\"}", mbpath);
	}

	/**
	 * 示例请求
	 * http://localhost:8433/mbtiles/mbtest1.mbtiles/metadata
	 * @param layer
	 * @return
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, path = "{layer}/metadata", produces = {
			"application/json;charset=UTF-8" })
	public MetadataEntry MbtileMetadata(@PathVariable String layer) {
		MBTilesReader mbTilesReader = _mbtileBean.getMBReader(layer).get_mBTileReader();
		if (mbTilesReader != null) {
			try {
				return mbTilesReader.getMetadata();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	@ResponseBody
	@RequestMapping(method = RequestMethod.GET, path = "datalist", produces = { "application/json;charset=UTF-8" })
	public List<MbtileInfo> MbtileIndex() {
		return _mbtileBean.getAllmbfiles();
	}

	@ResponseBody
	@RequestMapping(path = "refresh", method = RequestMethod.GET, produces = { "application/json;charset=UTF-8" })
	public UpdateStatus refreshFolder() {
		UpdateStatus _upstatus = new UpdateStatus();
		try {
			_mbtileBean.setmbtilesValue();
			_upstatus.isSuc = true;
		} catch (Exception e) {
			_upstatus.errorMsg = e.getMessage();
		}

		return _upstatus;
	}

	/**
	 * 示例请求：
	 * http://localhost:8433/mbtiles/mbtest1.mbtiles/7/105/79
	 * @param x
	 * @param y
	 * @param zoom
	 * @param layer
	 * @return
	 */
	@GetMapping("{layer}/{zoom}/{x}/{y}")
	public ResponseEntity getTile(@PathVariable int x, @PathVariable int y, @PathVariable int zoom,
			@PathVariable String layer) {
		try {
			MBTilesReader mbTilesReader = _mbtileBean.getMBReader(layer).get_mBTileReader();
			if (mbTilesReader != null) {
				com.wclwksn.mbtileimageserver.MbtileHandle.Tile tile = mbTilesReader.getTile(zoom, x, y);
				if (tile.getData() != null) {
					int size = tile.getData().available();
					byte[] bytes = new byte[size];
					tile.getData().read(bytes);
					HttpHeaders headers = new HttpHeaders();
					String _formattypeString = _mbtileBean.getMBReader(layer).get_imageFormat();
					if (_formattypeString.contains("pbf")) {
						headers.add("Content-Type", "application/x-protobuf");
						headers.add("Content-Encoding", "gzip");
					} else {
						headers.add("Content-Type", String.format("image/%s", _formattypeString));
					}
					return ResponseEntity.status(HttpStatus.OK).headers(headers).body(bytes);
				}
			}
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未找到对象");
		} catch (Exception e) {
			log.info(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(String.format("The tile with the parameters x=%d, y=%d & zoom=%d not found", x, y, zoom));
		}
	}

}
