package org.bitbucket.openkilda.messaging.command.flow;

import static com.google.common.base.MoreObjects.toStringHelper;

import org.bitbucket.openkilda.messaging.command.CommandData;
import org.bitbucket.openkilda.messaging.payload.request.FlowIdRequestPayload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Objects;

/**
 * Represents flow status northbound request.
 */
@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "command",
        "destination",
        "payload"})
public class FlowStatusRequest extends CommandData {
    /**
     * Serialization version number constant.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The request payload.
     */
    @JsonProperty("payload")
    protected FlowIdRequestPayload payload;

    /**
     * Default constructor.
     */
    public FlowStatusRequest() {
    }

    /**
     * Constructs instance.
     *
     * @param   payload  request payload
     *
     * @throws  IllegalArgumentException if payload is null
     */
    @JsonCreator
    public FlowStatusRequest(@JsonProperty("payload") final FlowIdRequestPayload payload) {
        setPayload(payload);
    }

    /**
     * Returns request payload.
     *
     * @return  request payload
     */
    public FlowIdRequestPayload getPayload() {
        return payload;
    }

    /**
     * Sets request payload.
     *
     * @param   payload  request payload
     */
    public void setPayload(final FlowIdRequestPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("need to set payload");
        }
        this.payload = payload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toStringHelper(this)
                .add("payload", payload)
                .toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(payload);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        FlowStatusRequest that = (FlowStatusRequest) object;
        return Objects.equals(getPayload(), that.getPayload());
    }
}
