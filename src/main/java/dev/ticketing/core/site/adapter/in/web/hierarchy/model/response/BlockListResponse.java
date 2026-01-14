package dev.ticketing.core.site.adapter.in.web.hierarchy.model.response;

import dev.ticketing.core.site.domain.hierarchy.Block;
import java.util.List;

public record BlockListResponse(List<BlockResponse> blocks) {
    public static BlockListResponse from(List<Block> blocks) {
        return new BlockListResponse(blocks.stream().map(BlockResponse::from).toList());
    }

    public record BlockResponse(Long id, String name) {
        public static BlockResponse from(Block block) {
            return new BlockResponse(block.getId(), block.getName());
        }
    }
}
