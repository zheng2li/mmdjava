/* 
 * PROJECT: MMD for Java
 * --------------------------------------------------------------------------------
 * This work is based on the ARTK_MMD v0.1 
 *   PY
 * http://ppyy.hp.infoseek.co.jp/
 * py1024<at>gmail.com
 * http://www.nicovideo.jp/watch/sm7398691
 *
 * The MMD for Java is Java version MMD class library.
 * Copyright (C)2009 nyatla
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this framework; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * For further information please contact.
 *	http://nyatla.jp/
 *	<airmail(at)ebony.plala.or.jp>
 * 
 */
package jp.nyatla.nymmd.core;


import jp.nyatla.nymmd.struct.pmd.PMD_Bone;
import jp.nyatla.nymmd.types.*;

public class PmdBone
{
	private String _name;
	private final MmdVector3 _pmd_bone_position=new MmdVector3();
	private final MmdVector3 m_vec3Offset=new MmdVector3();
	private final MmdMatrix m_matInvTransform=new MmdMatrix();	// åˆ�æœŸå€¤ã�®ãƒœãƒ¼ãƒ³ã‚’åŽŸç‚¹ã�«ç§»å‹•ã�•ã�›ã‚‹ã‚ˆã�†ã�ªè¡Œåˆ—

	private PmdBone	_parent_bone;
	private PmdBone	m_pChildBone;

	// ä»¥ä¸‹ã�¯ç�¾åœ¨ã�®å€¤
	public final MmdMatrix m_matLocal=new MmdMatrix();


	//å¼·åˆ¶public
	public final MmdVector3 m_vec3Position=new MmdVector3();
	public final MmdVector4 m_vec4Rotate=new MmdVector4();
	public boolean m_bIKLimitAngle;	// IKæ™‚ã�«è§’åº¦åˆ¶é™�ã‚’ã�™ã‚‹ã�‹ã�©ã�†ã�‹
	//å¼·åˆ¶public/
	
	public String getName()
	{
		return this._name;
	}
	
	public PmdBone(PMD_Bone pPMDBoneData,PmdBone[] pBoneArray)
	{
		// ãƒœãƒ¼ãƒ³å��ã�®ã‚³ãƒ”ãƒ¼
		this._name=pPMDBoneData.szName;

		// ä½�ç½®ã�®ã‚³ãƒ”ãƒ¼
		this._pmd_bone_position.setValue(pPMDBoneData.vec3Position);

		// è¦ªãƒœãƒ¼ãƒ³ã�®è¨­å®š
		if( pPMDBoneData.nParentNo != -1 )
		{
			this._parent_bone = pBoneArray[pPMDBoneData.nParentNo];
			m_vec3Offset.Vector3Sub(this._pmd_bone_position,this._parent_bone._pmd_bone_position);
		}else{
			// è¦ªã�ªã�—
			this._parent_bone=null;
			this.m_vec3Offset.setValue(this._pmd_bone_position);
		}

		// å­�ãƒœãƒ¼ãƒ³ã�®è¨­å®š
		if( pPMDBoneData.nChildNo != -1 )
		{
			this.setM_pChildBone(pBoneArray[pPMDBoneData.nChildNo]);
		}

		this.m_matInvTransform.MatrixIdentity();
		this.m_matInvTransform.m[3][0] = -this._pmd_bone_position.x; 
		this.m_matInvTransform.m[3][1] = -this._pmd_bone_position.y; 
		this.m_matInvTransform.m[3][2] = -this._pmd_bone_position.z; 

		this.m_bIKLimitAngle = false;

		// å�„å¤‰æ•°ã�®åˆ�æœŸå€¤ã‚’è¨­å®š
		reset();		
	}
	public void recalcOffset()
	{
		if(this._parent_bone!=null){
			m_vec3Offset.Vector3Sub(this._pmd_bone_position,this._parent_bone._pmd_bone_position);
		}
		return;	
	}

	public void reset()
	{
		m_vec3Position.x = m_vec3Position.y = m_vec3Position.z = 0.0f;
		m_vec4Rotate.x = m_vec4Rotate.y = m_vec4Rotate.z = 0.0f; m_vec4Rotate.w = 1.0f;

		this.m_matLocal.MatrixIdentity();
		this.m_matLocal.m[3][0] = _pmd_bone_position.x; 
		this.m_matLocal.m[3][1] = _pmd_bone_position.y; 
		this.m_matLocal.m[3][2] = _pmd_bone_position.z; 		
	}
	public void setIKLimitAngle(boolean i_value)
	{
		this.m_bIKLimitAngle=i_value;
		return;
	}

	
	public void updateSkinningMat(MmdMatrix o_matrix)
	{
		o_matrix.MatrixMultiply(this.m_matInvTransform,this.m_matLocal);		
		return;
	}
	public 	void updateMatrix()
	{
		// ã‚¯ã‚©ãƒ¼ã‚¿ãƒ‹ã‚ªãƒ³ã�¨ç§»å‹•å€¤ã�‹ã‚‰ãƒœãƒ¼ãƒ³ã�®ãƒ­ãƒ¼ã‚«ãƒ«ãƒžãƒˆãƒªãƒƒã‚¯ã‚¹ã‚’ä½œæˆ�
		this.m_matLocal.QuaternionToMatrix(this.m_vec4Rotate );
		this.m_matLocal.m[3][0] = m_vec3Position.x + m_vec3Offset.x; 
		this.m_matLocal.m[3][1] = m_vec3Position.y + m_vec3Offset.y; 
		this.m_matLocal.m[3][2] = m_vec3Position.z + m_vec3Offset.z; 

		// è¦ªã�Œã�‚ã‚‹ã�ªã‚‰è¦ªã�®å›žè»¢ã‚’å�—ã�‘ç¶™ã��
		if(this._parent_bone!=null){
			m_matLocal.MatrixMultiply(m_matLocal,this._parent_bone.m_matLocal);
		}
		return;
	}
	private final MmdMatrix _lookAt_matTemp=new MmdMatrix();
	private final MmdMatrix _lookAt_matInvTemp=new MmdMatrix();
	private final MmdVector3 _lookAt_vec3LocalTgtPosZY=new MmdVector3();
	private final MmdVector3 _lookAt_vec3LocalTgtPosXZ=new MmdVector3();
	private final MmdVector3 _lookAt_vec3Angle=new MmdVector3();
	
	
	
	public 	void lookAt(MmdVector3 pvecTargetPos )
	{
		// ã�©ã�†ã‚‚ã�Šã�‹ã�—ã�„ã�®ã�§è¦�èª¿æ•´
		final MmdMatrix matTemp=this._lookAt_matTemp;
		final MmdMatrix matInvTemp=this._lookAt_matInvTemp;
		final MmdVector3		vec3LocalTgtPosZY=this._lookAt_vec3LocalTgtPosZY;
		final MmdVector3		vec3LocalTgtPosXZ=this._lookAt_vec3LocalTgtPosXZ;

		matTemp.MatrixIdentity();
		matTemp.m[3][0] = m_vec3Position.x + m_vec3Offset.x; 
		matTemp.m[3][1] = m_vec3Position.y + m_vec3Offset.y; 
		matTemp.m[3][2] = m_vec3Position.z + m_vec3Offset.z;

		if(this._parent_bone!=null)
		{
			matInvTemp.MatrixInverse(_parent_bone.m_matLocal );
			matTemp.MatrixMultiply(matTemp, matInvTemp );
		}
		matTemp.MatrixInverse(matTemp);


		vec3LocalTgtPosZY.Vector3Transform(pvecTargetPos, matTemp );

		vec3LocalTgtPosXZ.setValue(vec3LocalTgtPosZY);
		vec3LocalTgtPosXZ.y = 0.0f;
		vec3LocalTgtPosXZ.Vector3Normalize(vec3LocalTgtPosXZ);

		vec3LocalTgtPosZY.x = 0.0f;
		vec3LocalTgtPosZY.Vector3Normalize(vec3LocalTgtPosZY);

		MmdVector3 vec3Angle = this._lookAt_vec3Angle;
		vec3Angle.x=vec3Angle.y=vec3Angle.z=0;

		if( vec3LocalTgtPosZY.z > 0.0f ){
            vec3Angle.x =  (float)(Math.asin(vec3LocalTgtPosZY.y ) - (20.0*Math.PI/180.0));
		}
		if( vec3LocalTgtPosXZ.x < 0.0f ){
			vec3Angle.y =  (float)Math.acos(vec3LocalTgtPosXZ.z );
		}else{
			vec3Angle.y = (float)-Math.acos(vec3LocalTgtPosXZ.z );
		}

		if( vec3Angle.x < (-25.0*Math.PI/180.0) ){
			vec3Angle.x = (float)(-25.0*Math.PI/180.0);
		}
		if( (45.0f*Math.PI/180.0) < vec3Angle.x  )	{
			vec3Angle.x = (float)( 45.0*Math.PI/180.0);
		}
		if( vec3Angle.y < (-80.0*Math.PI/180.0) ){
			vec3Angle.y = (float)(-80.0*Math.PI/180.0);
		}
		if((80.0*Math.PI/180.0) < vec3Angle.y  ){
			vec3Angle.y =(float)( 80.0*Math.PI/180.0);
		}

		m_vec4Rotate.QuaternionCreateEuler(vec3Angle);
	}

	public void setM_pChildBone(PmdBone m_pChildBone) {
		this.m_pChildBone = m_pChildBone;
	}

	public PmdBone getM_pChildBone() {
		return m_pChildBone;
	}
}
