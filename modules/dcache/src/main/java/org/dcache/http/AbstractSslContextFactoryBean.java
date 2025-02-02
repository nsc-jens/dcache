/* dCache - http://www.dcache.org/
 *
 * Copyright (C) 2021 Deutsches Elektronen-Synchrotron
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dcache.http;

import eu.emi.security.authn.x509.CrlCheckingMode;
import eu.emi.security.authn.x509.OCSPCheckingMode;
import java.nio.file.Path;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

abstract class AbstractSslContextFactoryBean<C> implements FactoryBean<C> {

    protected Path serverCertificatePath;
    protected Path serverKeyPath;
    protected Path serverCaPath;
    protected CrlCheckingMode crlCheckingMode;
    protected OCSPCheckingMode ocspCheckingMode;

    @Required
    public void setServerCertificatePath(Path serverCertificatePath) {
        this.serverCertificatePath = serverCertificatePath;
    }

    @Required
    public void setServerKeyPath(Path serverKeyPath) {
        this.serverKeyPath = serverKeyPath;
    }

    @Required
    public void setServerCaPath(Path serverCaPath) {
        this.serverCaPath = serverCaPath;
    }

    @Required
    public void setCrlCheckingMode(CrlCheckingMode crlCheckingMode) {
        this.crlCheckingMode = crlCheckingMode;
    }

    @Required
    public void setOcspCheckingMode(OCSPCheckingMode ocspCheckingMode) {
        this.ocspCheckingMode = ocspCheckingMode;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
