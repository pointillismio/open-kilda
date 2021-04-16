/* Copyright 2021 Telstra Open Source
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.openkilda.model;

import org.openkilda.model.FlowMirrorPath.FlowMirrorPathData;
import org.openkilda.model.cookie.FlowSegmentCookie;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.serializers.BeanSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a flow mirror path.
 */
@DefaultSerializer(BeanSerializer.class)
@ToString
public class FlowMirrorPath implements CompositeDataEntity<FlowMirrorPathData> {
    @Getter
    @Setter
    @Delegate
    @JsonIgnore
    private FlowMirrorPathData data;

    /**
     * No args constructor for deserialization purpose.
     */
    private FlowMirrorPath() {
        data = new FlowMirrorPathDataImpl();
    }

    /**
     * Cloning constructor which performs deep copy of the entity.
     *
     * @param entityToClone the path entity to copy data from.
     */
    public FlowMirrorPath(@NonNull FlowMirrorPath entityToClone, FlowMirrorPoints flowMirrorPoints) {
        data = FlowMirrorPathCloner.INSTANCE.deepCopy(entityToClone.getData(), flowMirrorPoints);
    }

    @Builder
    public FlowMirrorPath(@NonNull PathId pathId, @NonNull Switch srcSwitch, @NonNull Switch destSwitch,
                          int destPort, int destVlan, FlowSegmentCookie cookie, long bandwidth, boolean ignoreBandwidth,
                          FlowPathStatus status, List<PathSegment> segments, boolean destWithMultiTable) {
        data = FlowMirrorPathDataImpl.builder().pathId(pathId).srcSwitch(srcSwitch)
                .destSwitch(destSwitch).destPort(destPort).destVlan(destVlan).cookie(cookie).bandwidth(bandwidth)
                .ignoreBandwidth(ignoreBandwidth).status(status).destWithMultiTable(destWithMultiTable)
                .build();
        // The reference is used to link path segments back to the mirror path. See {@link #setSegments(List)}.
        ((FlowMirrorPathDataImpl) data).flowPath = this;

        if (segments != null && !segments.isEmpty()) {
            data.setSegments(segments);
        }
    }

    public FlowMirrorPath(@NonNull FlowMirrorPath.FlowMirrorPathData data) {
        this.data = data;
    }

    /**
     * Checks whether the flow path goes through a single switch.
     *
     * @return true if source and destination switches are the same, otherwise false
     */
    public boolean isSingleSwitchPath() {
        return getSrcSwitchId().equals(getDestSwitchId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FlowMirrorPath that = (FlowMirrorPath) o;
        return new EqualsBuilder()
                .append(getBandwidth(), that.getBandwidth())
                .append(isIgnoreBandwidth(), that.isIgnoreBandwidth())
                .append(getPathId(), that.getPathId())
                .append(getSrcSwitchId(), that.getSrcSwitchId())
                .append(getDestSwitchId(), that.getDestSwitchId())
                .append(getDestPort(), that.getDestPort())
                .append(getCookie(), that.getCookie())
                .append(getTimeCreate(), that.getTimeCreate())
                .append(getTimeModify(), that.getTimeModify())
                .append(getStatus(), that.getStatus())
                .append(getSegments(), that.getSegments())
                .append(isDestWithMultiTable(), that.isDestWithMultiTable())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPathId(), getSrcSwitchId(), getDestSwitchId(), getDestPort(), getBandwidth(),
                isIgnoreBandwidth(), getTimeCreate(), getTimeModify(), getStatus(), getSegments(),
                isDestWithMultiTable());
    }

    /**
     * Defines persistable data of the FlowMirrorPath.
     */
    public interface FlowMirrorPathData {
        PathId getPathId();

        void setPathId(PathId pathId);

        SwitchId getSrcSwitchId();

        Switch getSrcSwitch();

        void setSrcSwitch(Switch srcSwitch);

        SwitchId getDestSwitchId();

        Switch getDestSwitch();

        void setDestSwitch(Switch destSwitch);

        int getDestPort();

        void setDestPort(int destPort);

        int getDestVlan();

        void setDestVlan(int destVlan);

        FlowSegmentCookie getCookie();

        void setCookie(FlowSegmentCookie cookie);

        long getBandwidth();

        void setBandwidth(long bandwidth);

        boolean isIgnoreBandwidth();

        void setIgnoreBandwidth(boolean ignoreBandwidth);

        Instant getTimeCreate();

        void setTimeCreate(Instant timeCreate);

        Instant getTimeModify();

        void setTimeModify(Instant timeModify);

        FlowPathStatus getStatus();

        void setStatus(FlowPathStatus status);

        List<PathSegment> getSegments();

        void setSegments(List<PathSegment> segments);

        boolean isDestWithMultiTable();

        void setDestWithMultiTable(boolean destWithMultiTable);

        FlowMirrorPoints getFlowMirrorPoints();
    }

    /**
     * POJO implementation of FlowPathData.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static final class FlowMirrorPathDataImpl implements FlowMirrorPathData, Serializable {
        private static final long serialVersionUID = 1L;
        @NonNull PathId pathId;
        @NonNull Switch srcSwitch;
        @NonNull Switch destSwitch;
        int destPort;
        int destVlan;
        @Setter(AccessLevel.NONE)
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        FlowMirrorPoints flowMirrorPoints;
        FlowSegmentCookie cookie;
        long bandwidth;
        boolean ignoreBandwidth;
        Instant timeCreate;
        Instant timeModify;
        FlowPathStatus status;
        @Builder.Default
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        @NonNull List<PathSegment> segments = new ArrayList<>();

        public void setPathId(PathId pathId) {
            this.pathId = pathId;

            if (segments != null) {
                segments.forEach(segment -> segment.getData().setPathId(pathId));
            }
        }

        // The reference is used to link path segments back to the mirror path. See {@link #setSegments(List)}.
        @Setter(AccessLevel.NONE)
        @Getter(AccessLevel.NONE)
        @ToString.Exclude
        @EqualsAndHashCode.Exclude
        FlowMirrorPath flowPath;

        boolean destWithMultiTable;

        @Override
        public SwitchId getSrcSwitchId() {
            return srcSwitch.getSwitchId();
        }

        @Override
        public SwitchId getDestSwitchId() {
            return destSwitch.getSwitchId();
        }

        public void setBandwidth(long bandwidth) {
            this.bandwidth = bandwidth;

            if (segments != null) {
                segments.forEach(segment -> segment.getData().setBandwidth(bandwidth));
            }
        }

        public void setIgnoreBandwidth(boolean ignoreBandwidth) {
            this.ignoreBandwidth = ignoreBandwidth;

            if (segments != null) {
                segments.forEach(segment -> segment.getData().setIgnoreBandwidth(ignoreBandwidth));
            }
        }

        @Override
        public List<PathSegment> getSegments() {
            return Collections.unmodifiableList(segments);
        }

        /**
         * Set the segments.
         */
        @Override
        public void setSegments(List<PathSegment> segments) {
            for (int idx = 0; idx < segments.size(); idx++) {
                PathSegment segment = segments.get(idx);
                PathSegment.PathSegmentData data = segment.getData();
                data.setPathId(pathId);
                data.setSeqId(idx);
                data.setIgnoreBandwidth(ignoreBandwidth);
                data.setBandwidth(bandwidth);
            }

            this.segments = new ArrayList<>(segments);
        }
    }

    /**
     * A cloner for FlowPath entity.
     */
    @Mapper(collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE)
    public interface FlowMirrorPathCloner {
        FlowMirrorPathCloner INSTANCE = Mappers.getMapper(FlowMirrorPathCloner.class);

        void copy(FlowMirrorPathData source, @MappingTarget FlowMirrorPathData target);

        @Mapping(target = "srcSwitch", ignore = true)
        @Mapping(target = "destSwitch", ignore = true)
        @Mapping(target = "segments", ignore = true)
        void copyWithoutSwitchesAndSegments(FlowMirrorPathData source, @MappingTarget FlowMirrorPathData target);

        /**
         * Performs deep copy of entity data.
         *
         * @param source the path data to copy from.
         */
        default FlowMirrorPathData deepCopy(FlowMirrorPathData source, FlowMirrorPoints flowMirrorPoints) {
            FlowMirrorPathDataImpl result = new FlowMirrorPathDataImpl();
            result.flowMirrorPoints = flowMirrorPoints;
            copyWithoutSwitchesAndSegments(source, result);
            result.setSrcSwitch(new Switch(source.getSrcSwitch()));
            result.setDestSwitch(new Switch(source.getDestSwitch()));
            result.setSegments(source.getSegments().stream()
                    .map(PathSegment::new)
                    .collect(Collectors.toList()));
            return result;
        }
    }
}
