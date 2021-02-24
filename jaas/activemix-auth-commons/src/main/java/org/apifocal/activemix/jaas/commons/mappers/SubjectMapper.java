/*
 * Copyright (c) 2017-2020 apifocal LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apifocal.activemix.jaas.commons.mappers;

import com.nimbusds.jwt.JWTClaimsSet;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import org.apifocal.activemix.jaas.commons.Settings;
import org.apifocal.activemix.jaas.commons.SubjectPrincipal;
import org.apifocal.activemix.jaas.commons.ClaimMapper;

/**
 * Mapper of subject attribute from token.
 */
public class SubjectMapper implements ClaimMapper {

    public SubjectMapper(Settings settings) {

    }

    @Override
    public Set<Principal> map(JWTClaimsSet claimsSet) {
        return Collections.singleton(new SubjectPrincipal(claimsSet.getSubject()));
    }

}
