/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.bannerpack.repo;

import java.util.List;

public record RemoteRepoIndex(int repoVersion, String generatedAt, String baseUrl, List<RemotePackEntry> packs) {}
