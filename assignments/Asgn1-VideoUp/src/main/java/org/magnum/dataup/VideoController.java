package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

	private VideoFileManager videoFileManager;

	@ResponseStatus(value = org.springframework.http.HttpStatus.NOT_FOUND)
	public final class ResourceNotFoundException extends RuntimeException {

	}

	@PostConstruct
	public void initVideoFileManager() throws IOException {
		videoFileManager = VideoFileManager.get();
	}

	@RequestMapping(value = "/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideo() throws IOException {
		return videoFileManager.getVideos().values();
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
	public @ResponseBody HttpServletResponse getVideoData(
			@PathVariable("id") long id, HttpServletResponse response)
			throws IOException {
		Video video = videoFileManager.getVideos().get(id);
		if (video != null) {
			serveSomeVideo(video, response);
		} else {
			throw new ResourceNotFoundException();
		}
		return response;
	}

	@RequestMapping(value = "/video", method = RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video video)
			throws IOException {
		videoFileManager.save(video);
		video.setDataUrl(getDataUrl(video.getId()));
		return video;
	}

	@RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
	public @ResponseBody VideoStatus addVideoData(@PathVariable("id") long id,
			@RequestParam("data") MultipartFile data) throws IOException {

		Video video = videoFileManager.getVideos().get(id);
		if (video != null) {
			if (!data.isEmpty()) {
				VideoFileManager.get().saveVideoData(video,
						data.getInputStream());
			}
		} else {
			throw new ResourceNotFoundException();
		}

		return new VideoStatus(VideoState.READY);
	}

	public void serveSomeVideo(Video v, HttpServletResponse response)
			throws IOException {
		videoFileManager.copyVideoData(v, response.getOutputStream());
	}

	private String getDataUrl(long videoId) {
		String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
		return url;
	}

	private String getUrlBaseForLocalServer() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes()).getRequest();
		String base = "http://"
				+ request.getServerName()
				+ ((request.getServerPort() != 80) ? ":"
						+ request.getServerPort() : "");
		return base;
	}
}
