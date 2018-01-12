/**
 * Copyright (C) 2014 Karlsruhe Institute of Technology
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.kit.masi.pid.db;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.eclipse.persistence.queries.FetchGroupTracker;
import org.eclipse.persistence.sessions.Session;

/**
 * Links a PID to a digital object via its identifier.
 *
 * @author hartmann-v
 */
@Entity
//@XmlNamedObjectGraphs({
//    @XmlNamedObjectGraph(
//            name = "default",
//            attributeNodes = {
//                @XmlNamedAttributeNode("pid"),
//                @XmlNamedAttributeNode("digitalObjectIdentifier")
//            })})
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
@NamedEntityGraphs({
    @NamedEntityGraph(
            name = "Pid.default",
            includeAllAttributes = false,
            attributeNodes = {
                @NamedAttributeNode("pid"),
                @NamedAttributeNode("digitalObjectIdentifier")
            })
})
public class PidToDigitalObject implements Serializable, FetchGroupTracker {

    /**
     * UID should be the date of the last change in the format yyyyMMdd.
     */
    private static final long serialVersionUID = 20170626L;
    // <editor-fold defaultstate="collapsed" desc="declaration of variables">
    /**
     * Primary key of the schema.
     */
    @Id
    private String pid = null;

    /**
     * MetaDataSchema URL pointing to the schema.
     */
    @Column(nullable = false)
    private String digitalObjectIdentifier;


    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="constructors">
    /**
     * Default constructor. Not accessible from outside.
     */
    public PidToDigitalObject() {
        // this constructor is useless so it's forbidden for the users.
    }

    /**
     * Constructor only allow to set PID. This constructor is intended to
     * be used for search queries by PID.
     *
     * @param pPid The PID.
     */
    public PidToDigitalObject(String pPid) {
        this(pPid, "");
    }

    /**
     * Create a new MetaDataSchema.
     *
     * @param pPid The PID.
     * @param pDigitalObjectIdentifier The ID of the linked digital object.
     */
    public PidToDigitalObject(String pPid, String pDigitalObjectIdentifier) {
      pid = pPid;
      digitalObjectIdentifier = pDigitalObjectIdentifier;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="setters and getters">
    /**
     * Get PID.
     * @return pid of instance.
     */
    public String getPid() {
        return pid;
    }

    /**
     * Set PID.
     *
     * @param pPid PID.
     */
    public void setPid(String pPid) {
        this.pid = pPid;
    }
    /**
     * Get DigitalObjectIdentifier linked to the PID.
     * @return digital object identifier.
     */
    public String getDigitalObjectIdentifier() {
        return digitalObjectIdentifier;
    }

    /**
     * Set the digital object identifier.
     *
     * @param pDigitalObjectIdentifier The digital object identifier
     */
    public void setDigitalObjectIdentifier(String pDigitalObjectIdentifier) {
        this.digitalObjectIdentifier = pDigitalObjectIdentifier;
    }

    // </editor-fold>

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder("PID\n----\n");
        buffer.append("PID: ").append(getPid()).append(" -->  ").append(getDigitalObjectIdentifier());

        return buffer.toString();
    }

    @Override
    public boolean equals(Object other) {
        boolean equals = true;
        if (this == other) {
            return equals;
        }
        if (other != null && (getClass() == other.getClass())) {
            PidToDigitalObject otherMetaDataSchema = (PidToDigitalObject) other;
            if (pid != null) {
                equals = equals && (pid.equals(otherMetaDataSchema.pid));
            } else {
                equals = equals && (otherMetaDataSchema.pid == null);
            }
            if (equals && (digitalObjectIdentifier != null)) {
                equals = equals && (digitalObjectIdentifier.equals(otherMetaDataSchema.digitalObjectIdentifier));
            } else {
                equals = equals && (otherMetaDataSchema.digitalObjectIdentifier == null);
            }
        } else {
            equals = false;
        }

        return equals;
    }

    @Override
    public int hashCode() {
        int hash = 11;
        hash = 43 * hash + (this.pid != null ? this.pid.hashCode() : 0);
        hash = 43 * hash + (this.digitalObjectIdentifier != null ? this.digitalObjectIdentifier.hashCode() : 0);
        return hash;
    }

    private transient org.eclipse.persistence.queries.FetchGroup fg;
    private transient Session sn;

    @Override
    public org.eclipse.persistence.queries.FetchGroup _persistence_getFetchGroup() {
        return this.fg;
    }

    @Override
    public void _persistence_setFetchGroup(org.eclipse.persistence.queries.FetchGroup fg) {
        this.fg = fg;
    }

    @Override
    public boolean _persistence_isAttributeFetched(String string) {
        return true;
    }

    @Override
    public void _persistence_resetFetchGroup() {
    }

    @Override
    public boolean _persistence_shouldRefreshFetchGroup() {
        return false;
    }

    @Override
    public void _persistence_setShouldRefreshFetchGroup(boolean bln) {

    }

    @Override
    public Session _persistence_getSession() {

        return sn;
    }

    @Override
    public void _persistence_setSession(Session sn) {
        this.sn = sn;

    }
}
