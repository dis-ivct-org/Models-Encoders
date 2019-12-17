/*******************************************************************************
 * Copyright (C) Her Majesty the Queen in Right of Canada, 
 * as represented by the Minister of National Defence, 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package de.fraunhofer.iosb.tc_lib.converter;

import ca.drdc.ivct.fom.base.ParameterValue;
import ca.drdc.ivct.fom.base.structs.*;
import ca.drdc.ivct.fom.warfare.MunitionDetonation;
import ca.drdc.ivct.fom.warfare.WeaponFire;
import edu.nps.moves.dis.*;
import edu.nps.moves.disenum.DeadReckoningAlgorithm;
import edu.nps.moves.siso.EnumNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.drdc.ivct.fom.base.BaseEntity;
import ca.drdc.ivct.fom.base.SpatialRepresentation;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Convert from dis BaseEntity model to a GrimRPR representation
 */
public class DisModelConverter {

    private static Logger logger = LoggerFactory.getLogger(DisModelConverter.class);
    
    /**
     * Private construction to prevent instantiation
     */
    private DisModelConverter() {
    }

    /**
     * Convert an EntityStatePdu to a GrimRPR representation of BaseEntity
     * @param entitystatePDU a DIS pdu
     * @return BaseEntity a generic baseEntity
     */
    public static BaseEntity disEntityToRpr(EntityStatePdu entitystatePDU) {
        BaseEntity receivedEntity = new BaseEntity();

        EntityID id = entitystatePDU.getEntityID();
        EntityType type = entitystatePDU.getEntityType();

        receivedEntity.setEntityIdentifier(new EntityIdentifierStruct(id.getSite(), id.getApplication(), id.getEntity()));

        receivedEntity.setEntityType(
                new ca.drdc.ivct.fom.base.structs.EntityTypeStruct(
                        type.getEntityKind(),
                        type.getDomain(),
                        type.getCountry(),
                        type.getCategory(),
                        type.getSubcategory(),
                        type.getSpec(),
                        type.getExtra()));

        SpatialRepresentation spatial = extractSpatialFromDisPdu(entitystatePDU);
        receivedEntity.setSpatial(spatial);

        return receivedEntity;
    }

    /**
     * Convert an GrimRPR representation of BaseEntity to a EntityStatePdu
     * @param baseEntity a Generic Base Entity
     * @return EntityStatePdu a Dis Base Entity
     */
    public static EntityStatePdu rprEntityToDis(BaseEntity baseEntity){
        EntityStatePdu receivedEntity = new EntityStatePdu();
        
        EntityID entityID = new EntityID();
        EntityIdentifierStruct entityIdentifier = baseEntity.getEntityIdentifier();
        entityID.setApplication(entityIdentifier.getApplication());
        entityID.setEntity(entityIdentifier.getEntity());
        entityID.setSite(entityIdentifier.getSite());
        receivedEntity.setEntityID(entityID);

        EntityType entityType = new EntityType();
        ca.drdc.ivct.fom.base.structs.EntityTypeStruct hlaEntityType = baseEntity.getEntityType();
        entityType.setCategory(hlaEntityType.getCategory());
        entityType.setCountry(hlaEntityType.getCountryCode());
        entityType.setDomain(hlaEntityType.getDomain());
        entityType.setEntityKind(hlaEntityType.getEntityKind());
        entityType.setExtra(hlaEntityType.getExtra());
        entityType.setSpec(hlaEntityType.getSpecific());
        entityType.setSubcategory(hlaEntityType.getSubcategory());
        receivedEntity.setEntityType(entityType);
        
        SpatialRepresentation spatialRepresentation = baseEntity.getSpatialRepresentation();
        
        WorldLocationStruct location = spatialRepresentation.getWorldLocation();
        Vector3Double locationVector = new Vector3Double();
        locationVector.setX(location.getxPosition());
        locationVector.setY(location.getyPosition());
        locationVector.setZ(location.getzPosition());
        receivedEntity.setEntityLocation(locationVector);

        Orientation orientation = new Orientation();
        orientation.setPhi(spatialRepresentation.getOrientation().getPhi());
        orientation.setPsi(spatialRepresentation.getOrientation().getPsi());
        orientation.setTheta(spatialRepresentation.getOrientation().getTheta());
        receivedEntity.setEntityOrientation(orientation);


        Vector3Float linearVelocity = new Vector3Float();
        linearVelocity.setX(spatialRepresentation.getVelocityVector().getxVelocity());
        linearVelocity.setY(spatialRepresentation.getVelocityVector().getyVelocity());
        linearVelocity.setZ(spatialRepresentation.getVelocityVector().getzVelocity());

        receivedEntity.setEntityLinearVelocity(linearVelocity);

        DeadReckoningParameter deadReckoningParameter = extractDeadReckoningParametersFromSpatiaRep(spatialRepresentation);
        receivedEntity.setDeadReckoningParameters(deadReckoningParameter);

        if (spatialRepresentation.isFrozen()) {
            receivedEntity.setEntityAppearance(1);
        }

        return receivedEntity;
    }

    public static WeaponFire disWeaponFireToRpr (FirePdu firePdu) {
        WeaponFire weaponFire = new WeaponFire();

        weaponFire.setMunitionObjectIdentifier(firePdu.getMunitionID().getSite()+"."+ firePdu.getMunitionID().getApplication()+"."+firePdu.getMunitionID().getEntity());

        EventIdentifierStruct eventIDStruct = new EventIdentifierStruct(firePdu.getEventID().getEventNumber(),firePdu.getEventID().getSite()+"."+firePdu.getEventID().getApplication());
        weaponFire.setEventIdentifier(eventIDStruct);

        weaponFire.setFiringObjectIdentifier(firePdu.getFiringEntityID().getSite()+"."+firePdu.getFiringEntityID().getApplication()+"."+firePdu.getFiringEntityID().getEntity());

        weaponFire.setFireMissionIndex(Integer.toUnsignedLong(firePdu.getFireMissionIndex()));

        WorldLocationStruct worldLocation = new WorldLocationStruct(firePdu.getLocationInWorldCoordinates().getX(), firePdu.getLocationInWorldCoordinates().getY(), firePdu.getLocationInWorldCoordinates().getZ());
        weaponFire.setFiringLocation(worldLocation);

        EntityType munitionType = firePdu.getBurstDescriptor().getMunition();
        EntityTypeStruct entityTypeStruct = new EntityTypeStruct(munitionType.getEntityKind(), munitionType.getDomain(), munitionType.getCountry(), munitionType.getCategory(), munitionType.getSubcategory(), munitionType.getSpec(), munitionType.getExtra());
        weaponFire.setMunitionType(entityTypeStruct);

        weaponFire.setWarheadType(firePdu.getBurstDescriptor().getWarhead());
        weaponFire.setFuseType(firePdu.getBurstDescriptor().getFuse());
        weaponFire.setQuantityFired(firePdu.getBurstDescriptor().getQuantity());
        weaponFire.setRateOfFire(firePdu.getBurstDescriptor().getRate());

        weaponFire.setTargetObjectIdentifier(firePdu.getTargetEntityID().getSite()+"."+firePdu.getTargetEntityID().getApplication()+"."+firePdu.getTargetEntityID().getEntity());

        VelocityVectorStruct initialVelocity = new VelocityVectorStruct(firePdu.getVelocity().getX(), firePdu.getVelocity().getY(), firePdu.getVelocity().getZ());
        weaponFire.setInitialVelocityVector(initialVelocity);

        weaponFire.setFireControlSolutionRange(firePdu.getRangeToTarget());
        return weaponFire;
    }

    public static FirePdu rprWeaponFireToDIS(WeaponFire weaponFire) {
        FirePdu firePdu = new FirePdu();

        EntityID munitionID = new EntityID();
        String[] splitMunitionID = weaponFire.getMunitionObjectIdentifier().split("\\.");
        munitionID.setSite(Integer.parseInt(splitMunitionID[0]));
        munitionID.setApplication(Integer.parseInt(splitMunitionID[1]));
        munitionID.setEntity(Integer.parseInt(splitMunitionID[2]));
        firePdu.setMunitionID(munitionID);

        EventID eventID = new EventID();
        String[] splitEventID = weaponFire.getEventIdentifier().getIssuingObjectIdentifier().split("\\.");
        eventID.setSite(Integer.parseInt(splitEventID[0]));
        eventID.setApplication(Integer.parseInt(splitEventID[1]));
        eventID.setEventNumber(weaponFire.getEventIdentifier().getEventCount());
        firePdu.setEventID(eventID);


        firePdu.setFireMissionIndex((int)( weaponFire.getFireMissionIndex()));

        Vector3Double locationInWorldCoords = new Vector3Double();
        locationInWorldCoords.setX(weaponFire.getFiringLocation().getxPosition());
        locationInWorldCoords.setY(weaponFire.getFiringLocation().getyPosition());
        locationInWorldCoords.setZ(weaponFire.getFiringLocation().getzPosition());
        firePdu.setLocationInWorldCoordinates(locationInWorldCoords);


        String[] splitFiringEntityID = weaponFire.getFiringObjectIdentifier().split("\\.");
        EntityID firingEntityID = new EntityID();
        firingEntityID.setSite(Integer.parseInt(splitFiringEntityID[0]));
        firingEntityID.setApplication(Integer.parseInt(splitFiringEntityID[1]));
        firingEntityID.setEntity(Integer.parseInt(splitFiringEntityID[2]));
        firePdu.setFiringEntityID(firingEntityID);

        BurstDescriptor burst = new BurstDescriptor();
        EntityType munitionType = new EntityType();
        munitionType.setEntityKind(weaponFire.getMunitionType().getEntityKind());
        munitionType.setDomain(weaponFire.getMunitionType().getDomain());
        munitionType.setCountry(weaponFire.getMunitionType().getCountryCode());
        munitionType.setCategory(weaponFire.getMunitionType().getCategory());
        munitionType.setSubcategory(weaponFire.getMunitionType().getSubcategory());
        munitionType.setSpec(weaponFire.getMunitionType().getSpecific());
        munitionType.setExtra(weaponFire.getMunitionType().getExtra());
        burst.setMunition(munitionType);
        burst.setWarhead(weaponFire.getWarheadType());
        burst.setFuse(weaponFire.getFuseType());
        burst.setQuantity(weaponFire.getQuantityFired());
        burst.setRate(weaponFire.getRateOfFire());
        firePdu.setBurstDescriptor(burst);

        Vector3Float initialVelocity = new Vector3Float();
        initialVelocity.setX(weaponFire.getInitialVelocityVector().getxVelocity());
        initialVelocity.setY(weaponFire.getInitialVelocityVector().getyVelocity());
        initialVelocity.setZ(weaponFire.getInitialVelocityVector().getzVelocity());
        firePdu.setVelocity(initialVelocity);

        String[] splitTargetEntityID = weaponFire.getTargetObjectIdentifier().split("\\.");
        EntityID targetEntityID = new EntityID();
        targetEntityID.setSite(Integer.parseInt(splitTargetEntityID[0]));
        targetEntityID.setApplication(Integer.parseInt(splitTargetEntityID[1]));
        targetEntityID.setEntity(Integer.parseInt(splitTargetEntityID[2]));
        firePdu.setTargetEntityID(targetEntityID);

        firePdu.setRangeToTarget(weaponFire.getFireControlSolutionRange());

        return firePdu;
    }



    public static MunitionDetonation disMunitionDetonationToRpr (DetonationPdu detonationPdu) {
        MunitionDetonation munitionDetonation = new MunitionDetonation();

        int articulationSize = detonationPdu.getArticulationParameters().size();
        ArticulatedParameterStruct[] articulatedParams = new ArticulatedParameterStruct[articulationSize];

        IntStream.range(0, articulationSize).forEach(i -> {

            ArticulationParameter disArticulationParam = detonationPdu.getArticulationParameters().get(i);

            ParameterValue paramValue;
            switch (disArticulationParam.getParameterTypeDesignator()) {

                case 0:
                    //Articulated
                    paramValue = new ArticulatedPartsStruct(disArticulationParam.getParameterType()-disArticulationParam.getParameterType()%32, disArticulationParam.getParameterType() & 0x1f, (float) disArticulationParam.getParameterValue());

                    break;
                case 1:
                    //Attached
                    double typeAsADouble = disArticulationParam.getParameterValue();
                    byte[] bytes = new byte[8];
                    ByteBuffer.wrap(bytes).putDouble(typeAsADouble);

                    //Bytes are inverted when read/written by the computer.
                    byte[] countryBytes = {bytes[3], bytes[2]};
                    short countryShort = ByteBuffer.wrap(countryBytes).getShort();
                    int countryInt = Short.toUnsignedInt(countryShort);
                    EntityTypeStruct entityType = new EntityTypeStruct(bytes[0], bytes[1], countryInt, bytes[4], bytes[5], bytes[6], bytes[7]);

                    paramValue = new AttachedPartsStruct(Integer.toUnsignedLong(disArticulationParam.getParameterType()), entityType);

                    break;
                default:
                    paramValue = null;
            }


            paramValue.setArticulatedParameterType(Integer.toUnsignedLong(disArticulationParam.getParameterType()));
            ArticulatedParameterStruct parameterStruct = new ArticulatedParameterStruct((byte) disArticulationParam.getChangeIndicator(), disArticulationParam.getPartAttachedTo(), paramValue);

            articulatedParams[i] = parameterStruct;
        });

        munitionDetonation.setArticulatedPartData(articulatedParams);

        WorldLocationStruct worldLocation = new WorldLocationStruct(detonationPdu.getLocationInWorldCoordinates().getX(), detonationPdu.getLocationInWorldCoordinates().getY(), detonationPdu.getLocationInWorldCoordinates().getZ());
        munitionDetonation.setDetonationLocation(worldLocation);

        munitionDetonation.setDetonationResultCode((byte) detonationPdu.getDetonationResult());

        String siteAndAppId = detonationPdu.getEventID().getSite() + "." + detonationPdu.getEventID().getApplication();
        munitionDetonation.setEventIdentifier(new EventIdentifierStruct(detonationPdu.getEventID().getEventNumber(), siteAndAppId));

        String firingObjID = detonationPdu.getFiringEntityID().getSite() + "." + detonationPdu.getFiringEntityID().getApplication() + "." + detonationPdu.getFiringEntityID().getEntity();
        munitionDetonation.setFiringObjectIdentifier(firingObjID);

        munitionDetonation.setMunitionObjectIdentifier(detonationPdu.getMunitionID().getSite()+"."+ detonationPdu.getMunitionID().getApplication()+"."+detonationPdu.getMunitionID().getEntity());

        VelocityVectorStruct finalVelocity = new VelocityVectorStruct(detonationPdu.getVelocity().getX(), detonationPdu.getVelocity().getY(), detonationPdu.getVelocity().getZ());
        munitionDetonation.setFinalVelocityVector(finalVelocity);

        munitionDetonation.setFuseType(detonationPdu.getBurstDescriptor().getFuse());

        String munitionObjID = detonationPdu.getMunitionID().getSite() + "." + detonationPdu.getMunitionID().getApplication() +"."+ detonationPdu.getMunitionID().getEntity();
        munitionDetonation.setMunitionObjectIdentifier(munitionObjID);

        EntityType munitionType = detonationPdu.getBurstDescriptor().getMunition();
        munitionDetonation.setMunitionType(new EntityTypeStruct(munitionType.getEntityKind(), munitionType.getDomain(), munitionType.getCountry(), munitionType.getCategory(), munitionType.getSubcategory(), munitionType.getSpec(), munitionType.getExtra()));

        munitionDetonation.setQuantityFired(detonationPdu.getBurstDescriptor().getQuantity());
        munitionDetonation.setRateOfFire(detonationPdu.getBurstDescriptor().getRate());

        RelativePositionStruct relativePosition = new RelativePositionStruct(detonationPdu.getLocationInEntityCoordinates().getX(), detonationPdu.getLocationInEntityCoordinates().getY(), detonationPdu.getLocationInEntityCoordinates().getZ());
        munitionDetonation.setRelativeDetonationLocation(relativePosition);


        String targetID =  detonationPdu.getTargetEntityID().getSite() +"."  + detonationPdu.getTargetEntityID().getApplication()+"." + detonationPdu.getTargetEntityID().getEntity();
        munitionDetonation.setTargetObjectIdentifier(targetID);

        munitionDetonation.setWarheadType(detonationPdu.getBurstDescriptor().getWarhead());

        return munitionDetonation;
    }

    public static DetonationPdu rprMunitionDetonationToDIS(MunitionDetonation munitionDetonation) {
        DetonationPdu detonationPdu = new DetonationPdu();

        EntityID munitionID = new EntityID();
        String[] splitMunitionID = munitionDetonation.getMunitionObjectIdentifier().split("\\.");
        munitionID.setSite(Integer.parseInt(splitMunitionID[0]));
        munitionID.setApplication(Integer.parseInt(splitMunitionID[1]));
        munitionID.setEntity(Integer.parseInt(splitMunitionID[2]));
        detonationPdu.setMunitionID(munitionID);

        String[] splitFiringEntityID = munitionDetonation.getFiringObjectIdentifier().split("\\.");
        EntityID firingEntityID = new EntityID();
        firingEntityID.setSite(Integer.parseInt(splitFiringEntityID[0]));
        firingEntityID.setApplication(Integer.parseInt(splitFiringEntityID[1]));
        firingEntityID.setEntity(Integer.parseInt(splitFiringEntityID[2]));
        detonationPdu.setFiringEntityID(firingEntityID);

        EventID eventID = new EventID();
        String[] splitEventID = munitionDetonation.getEventIdentifier().getIssuingObjectIdentifier().split("\\.");
        eventID.setSite(Integer.parseInt(splitEventID[0]));
        eventID.setApplication(Integer.parseInt(splitEventID[1]));
        eventID.setEventNumber(munitionDetonation.getEventIdentifier().getEventCount());
        detonationPdu.setEventID(eventID);

        Vector3Float finalVelocity = new Vector3Float();
        finalVelocity.setX(munitionDetonation.getFinalVelocityVector().getxVelocity());
        finalVelocity.setY(munitionDetonation.getFinalVelocityVector().getyVelocity());
        finalVelocity.setZ(munitionDetonation.getFinalVelocityVector().getzVelocity());
        detonationPdu.setVelocity(finalVelocity);

        Vector3Double locationInWorldCoords = new Vector3Double();
        locationInWorldCoords.setX(munitionDetonation.getDetonationLocation().getxPosition());
        locationInWorldCoords.setY(munitionDetonation.getDetonationLocation().getyPosition());
        locationInWorldCoords.setZ(munitionDetonation.getDetonationLocation().getzPosition());
        detonationPdu.setLocationInWorldCoordinates(locationInWorldCoords);

        BurstDescriptor burst = new BurstDescriptor();
        EntityType munitionType = new EntityType();
        munitionType.setEntityKind(munitionDetonation.getMunitionType().getEntityKind());
        munitionType.setDomain(munitionDetonation.getMunitionType().getDomain());
        munitionType.setCountry(munitionDetonation.getMunitionType().getCountryCode());
        munitionType.setCategory(munitionDetonation.getMunitionType().getCategory());
        munitionType.setSubcategory(munitionDetonation.getMunitionType().getSubcategory());
        munitionType.setSpec(munitionDetonation.getMunitionType().getSpecific());
        munitionType.setExtra(munitionDetonation.getMunitionType().getExtra());
        burst.setMunition(munitionType);
        burst.setWarhead(munitionDetonation.getWarheadType());
        burst.setFuse(munitionDetonation.getFuseType());
        burst.setQuantity(munitionDetonation.getQuantityFired());
        burst.setRate(munitionDetonation.getRateOfFire());
        detonationPdu.setBurstDescriptor(burst);

        Vector3Float locationInEntity = new Vector3Float();
        locationInEntity.setX(munitionDetonation.getRelativeDetonationLocation().getBodyXPosition());
        locationInEntity.setY(munitionDetonation.getRelativeDetonationLocation().getBodyYPosition());
        locationInEntity.setZ(munitionDetonation.getRelativeDetonationLocation().getBodyZPosition());
        detonationPdu.setLocationInEntityCoordinates(locationInEntity);

        String[] splitTargetEntityID = munitionDetonation.getTargetObjectIdentifier().split("\\.");
        EntityID targetEntityID = new EntityID();
        targetEntityID.setSite(Integer.parseInt(splitTargetEntityID[0]));
        targetEntityID.setApplication(Integer.parseInt(splitTargetEntityID[1]));
        targetEntityID.setEntity(Integer.parseInt(splitTargetEntityID[2]));
        detonationPdu.setTargetEntityID(targetEntityID);

        detonationPdu.setDetonationResult(munitionDetonation.getDetonationResultCode());

        detonationPdu.setNumberOfArticulationParameters((short) munitionDetonation.getArticulatedPartData().length);

        List<ArticulationParameter> articulationParameters = new ArrayList<>();
        Arrays.stream(munitionDetonation.getArticulatedPartData()).forEach(part -> {
            ArticulationParameter articulationParam = new ArticulationParameter();

            articulationParam.setChangeIndicator(part.getArticulatedParameterChange());
            articulationParam.setPartAttachedTo(part.getPartAttachedTo());



            if (part.getParameterValue().getClass().isAssignableFrom(ArticulatedPartsStruct.class)) {
                articulationParam.setParameterTypeDesignator((short) 0);

                ArticulatedPartsStruct articulatedPart = (ArticulatedPartsStruct) part.getParameterValue();

                articulationParam.setParameterValue(articulatedPart.getValue());

                articulationParam.setParameterType((int) ( articulatedPart.getTypeMetric() + articulatedPart.getArticulatedPartsType()) );

            } else if (part.getParameterValue().getClass().isAssignableFrom(AttachedPartsStruct.class)) {
                articulationParam.setParameterType((int)( part.getParameterValue().getArticulatedParameterType()));
                articulationParam.setParameterTypeDesignator((short) 1);
                EntityTypeStruct entityType = ((AttachedPartsStruct) part.getParameterValue()).getStoreType();

                byte[] totalEntity = new byte[8];
                totalEntity[0] = (byte) entityType.getEntityKind();
                totalEntity[1] = (byte) entityType.getDomain();

                //Byte shift to move from short to byte array. This way the country code is saved in the same 8 bytes array
                totalEntity[2] = (byte) (((short) entityType.getCountryCode()) & 0xff);
                totalEntity[3] = (byte) (((short) entityType.getCountryCode()>>> 8) & 0xff);


                totalEntity[4] = (byte) entityType.getCategory();
                totalEntity[5] = (byte) entityType.getSubcategory();
                totalEntity[6] = (byte) entityType.getSpecific();
                totalEntity[7] = (byte) entityType.getExtra();

                double entityTypeToDouble = ByteBuffer.wrap(totalEntity).getDouble();
                articulationParam.setParameterValue(entityTypeToDouble);

            }

            articulationParameters.add(articulationParam);


        });
        detonationPdu.setArticulationParameters(articulationParameters);



        return detonationPdu;
    }


    /**
     * Extract  dis DeadReckoningParameter of a spatial representation.
     * @param spatialRepresentation GrimRpr spatial representation
     * @return a dis DeadReckoningParameter
     */
    private static DeadReckoningParameter extractDeadReckoningParametersFromSpatiaRep(SpatialRepresentation spatialRepresentation) {

        DeadReckoningParameter pDeadReckoningParameters = new DeadReckoningParameter();
        
        DeadReckoningAlgorithm deadreckonAlgo;
        try {
            deadreckonAlgo = DeadReckoningAlgorithm.getEnumerationForValue(spatialRepresentation.getDeadReckoningAlgorithm().value());
        } catch (EnumNotFoundException e) {
            deadreckonAlgo = DeadReckoningAlgorithm.OTHER;
        }
        
        pDeadReckoningParameters.setDeadReckoningAlgorithm((short)deadreckonAlgo.value);

        switch (deadreckonAlgo) {
        case DRMF_P_W:
            // every step are done before the switch statement
            break;
        case DRMR_V_W:
            AccelerationVectorStruct accelerationVector = ((SpatialRVStruct)spatialRepresentation).getAccelerationVector();
            Vector3Float accelerationVector3Float = new Vector3Float();
            accelerationVector3Float.setX(accelerationVector.getxAcceleration());
            accelerationVector3Float.setY(accelerationVector.getyAcceleration());
            accelerationVector3Float.setZ(accelerationVector.getzAcceleration());
            pDeadReckoningParameters.setEntityLinearAcceleration(accelerationVector3Float);
            
            AngularVelocityVectorStruct angularVelocityVector = ((SpatialRVStruct)spatialRepresentation).getAngularVelocityVector();
            Vector3Float angularVelocityVector3Float = new Vector3Float();
            angularVelocityVector3Float.setX(angularVelocityVector.getxAngularVelocity());
            angularVelocityVector3Float.setY(angularVelocityVector.getyAngularVelocity());
            angularVelocityVector3Float.setZ(angularVelocityVector.getzAngularVelocity());
            pDeadReckoningParameters.setEntityAngularVelocity(angularVelocityVector3Float);
            break;
        default:
            logger.warn("Not supported yet");
            break;
        }
        return pDeadReckoningParameters;
    }

    /**
     * extratc a SpatialRepresentation from an EntityStatePdu
     * @param entityStatePdu the pdu to extract the spatial
     * @return a spatial representation
     */
    private static SpatialRepresentation extractSpatialFromDisPdu(EntityStatePdu entityStatePdu) {

        SpatialRepresentation spatiaLRepresentation = null; 
        Vector3Double location = entityStatePdu.getEntityLocation();
        WorldLocationStruct worldLocationStruct = new WorldLocationStruct(location.getX(), location.getY(),
                location.getZ());

        Orientation orientation = entityStatePdu.getEntityOrientation();
        OrientationStruct orientationStruct = new OrientationStruct(orientation.getPsi(), orientation.getTheta(),
                orientation.getPhi());

        Vector3Float linVelocity = entityStatePdu.getEntityLinearVelocity();
        VelocityVectorStruct velocityVectorStruct = new VelocityVectorStruct(linVelocity.getX(), linVelocity.getY(),
                linVelocity.getZ());

        DeadReckoningParameter deadReckonParam = entityStatePdu.getDeadReckoningParameters();

        DeadReckoningAlgorithm deadReckoningAlgorithm;
        try {
            deadReckoningAlgorithm = DeadReckoningAlgorithm.getEnumerationForValue(deadReckonParam.getDeadReckoningAlgorithm());
        } catch (EnumNotFoundException e) {
            deadReckoningAlgorithm = DeadReckoningAlgorithm.OTHER;
        }

        ca.drdc.ivct.fom.der.DeadReckoningAlgorithm genericDeadReckoningAlgorithm = ca.drdc.ivct.fom.der.DeadReckoningAlgorithm
                .valueOf((byte) deadReckoningAlgorithm.value);

        switch (deadReckoningAlgorithm) {
        case DRMF_P_W:
            spatiaLRepresentation = new SpatialFPStruct(genericDeadReckoningAlgorithm, worldLocationStruct, false, orientationStruct,
                    velocityVectorStruct);

            if (entityStatePdu.getEntityAppearance() == 1){
                ((SpatialFPStruct) spatiaLRepresentation).setFrozen(true);
            }

            break;
        case DRMR_V_W:

            Vector3Float angularVelocity = deadReckonParam.getEntityAngularVelocity();
            AngularVelocityVectorStruct angularVelocityStruct = new AngularVelocityVectorStruct(angularVelocity.getX(),
                    angularVelocity.getY(), angularVelocity.getZ());

            Vector3Float linearAcceleration = deadReckonParam.getEntityLinearAcceleration();
            AccelerationVectorStruct accelerationVectorStruct = new AccelerationVectorStruct(linearAcceleration.getX(),
                    linearAcceleration.getY(), linearAcceleration.getZ());

            spatiaLRepresentation = new SpatialRVStruct(genericDeadReckoningAlgorithm, worldLocationStruct, false, orientationStruct,
                    velocityVectorStruct, accelerationVectorStruct, angularVelocityStruct);

            if (entityStatePdu.getEntityAppearance() == 1){
                ((SpatialRVStruct) spatiaLRepresentation).setFrozen(true);
            }

            break;
        default:
            logger.warn("Not supported yet");
            break;
        }
        return spatiaLRepresentation;
    }
}
