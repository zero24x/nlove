package com.nlove.searcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jsmith.nknsdk.client.Identity;
import jsmith.nknsdk.client.NKNClient;

public class Searcher {

	private static final Logger LOG = LoggerFactory.getLogger(NKNClient.class);

	private List<String> matches;
	private String term;
	public static Path SHARE_DIR_PATH = Paths.get(System.getProperty("user.dir"), "share");

	public String searchFor(String term, Identity providerIdentity) {

		this.term = term;

		this.matches = new LinkedList<String>();

		Stream<Path> stream;
		try {
			stream = Files.find(Paths.get(System.getProperty("user.dir"), "share"), Integer.MAX_VALUE,
					(path, attrs) -> {
						if (!attrs.isRegularFile()) {
							return false;
						}
						if ((SHARE_DIR_PATH.relativize(path).toString().contains(term))) {
							return true;
						}

						return false;

					});

			this.matches = stream.map(x -> {
				String fileSize = FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(x.toFile()));

				return String.format("%s/%s (%s)", providerIdentity.getFullIdentifier(),
						SHARE_DIR_PATH.relativize(x).toString().replace("\\", "/"), fileSize);
			}).collect(Collectors.toList());

		} catch (

		IOException e) {
			LOG.error(e.toString());
		}

		if (this.matches.isEmpty()) {
			return "Nothing found!";
		}

		return String.join("\n", this.matches);
	}

}
