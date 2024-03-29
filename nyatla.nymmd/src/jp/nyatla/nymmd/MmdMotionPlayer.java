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
package jp.nyatla.nymmd;


import jp.nyatla.nymmd.core.PmdBone;
import jp.nyatla.nymmd.core.PmdFace;
import jp.nyatla.nymmd.core.PmdIK;
import jp.nyatla.nymmd.types.*;

public class MmdMotionPlayer
{
	private MmdPmdModel _ref_pmd_model;
	private MmdVmdMotion _ref_vmd_motion;

	private PmdBone[] m_ppBoneList;
	private PmdFace[] m_ppFaceList;

	private float m_fOldFrame,m_fFrame;
	private boolean m_bLoop;		// モーションをループするかどうか/if the animation loops
	private MmdMatrix[] _skinning_mat;

	private PmdBone m_pNeckBone;		// 首のボーン/neck bone


	private void getMotionPosRot(MotionData pMotionData, float fFrame, MmdVector3 pvec3Pos, MmdVector4 pvec4Rot)
	{
		int	i;
		int	ulNumKeyFrame = pMotionData.ulNumKeyFrames;

		// 最終フレームを過ぎていた場合/If it was after the last frame
		if( fFrame > pMotionData.pKeyFrames[ulNumKeyFrame - 1].fFrameNo )
		{
			fFrame = pMotionData.pKeyFrames[ulNumKeyFrame - 1].fFrameNo;
		}

		// 現在の時間がどのキー近辺にあるか/Is there any key near the current time
		for( i = 0 ; i < ulNumKeyFrame ; i++ )
		{
			if( fFrame <= pMotionData.pKeyFrames[i].fFrameNo )
			{
				break;
			}
		}

		// 前後のキーを設定/Set around the keyframe
		int	lKey0,
				lKey1;

		lKey0 = i - 1;
		lKey1 = i;

		if( lKey0 <= 0 )			lKey0 = 0;
		if( i == ulNumKeyFrame )	lKey1 = ulNumKeyFrame - 1;

		// 前後のキーの時間/Around the time of the keyframe
		float	fTime0 = pMotionData.pKeyFrames[lKey0].fFrameNo;
		float	fTime1 = pMotionData.pKeyFrames[lKey1].fFrameNo;

		// 前後のキーの間でどの位置にいるか/That the key to any position between front and rear
		float	fLerpValue;
		if( lKey0 != lKey1 )
		{
			fLerpValue = (fFrame - fTime0) / (fTime1 - fTime0);
			pvec3Pos.Vector3Lerp(pMotionData.pKeyFrames[lKey0].vec3Position,pMotionData.pKeyFrames[lKey1].vec3Position, fLerpValue);
			pvec4Rot.QuaternionSlerp(pMotionData.pKeyFrames[lKey0].vec4Rotate,pMotionData.pKeyFrames[lKey1].vec4Rotate, fLerpValue);
			pvec4Rot.QuaternionNormalize(pvec4Rot);//これほんとにいるの？/Really have this?
		}
		else
		{
			pvec3Pos.setValue(pMotionData.pKeyFrames[lKey0].vec3Position);
			pvec4Rot.setValue(pMotionData.pKeyFrames[lKey0].vec4Rotate);
		}		
	}

	private float getFaceRate(FaceData pFaceData, float fFrame)
	{
		int	i;
		int	ulNumKeyFrame = pFaceData.ulNumKeyFrames;

		// 最終フレームを過ぎていた場合/If it was after the last frame
		if( fFrame > pFaceData.pKeyFrames[ulNumKeyFrame - 1].fFrameNo )
		{
			fFrame = pFaceData.pKeyFrames[ulNumKeyFrame - 1].fFrameNo;
		}

		// 現在の時間がどのキー近辺にあるか/Is there any key near the current time
		for( i = 0 ; i < ulNumKeyFrame ; i++ )
		{
			if( fFrame <= pFaceData.pKeyFrames[i].fFrameNo )
			{
				break;
			}
		}

		// 前後のキーを設定/Set around the keyframe
		int lKey0 = i - 1;
		int lKey1 = i;

		if( lKey0 <= 0 ){
			lKey0 = 0;
		}
		if( i == ulNumKeyFrame ){
			lKey1 = ulNumKeyFrame - 1;
		}

		// 前後のキーの時間/Around the time of the keyframe
		float	fTime0 = pFaceData.pKeyFrames[lKey0].fFrameNo;
		float	fTime1 = pFaceData.pKeyFrames[lKey1].fFrameNo;

		// 前後のキーの間でどの位置にいるか/That the key to any position between front and rear
		float	fLerpValue;
		if( lKey0 != lKey1 )
		{
			fLerpValue = (fFrame - fTime0) / (fTime1 - fTime0);
			return (pFaceData.pKeyFrames[lKey0].fRate * (1.0f - fLerpValue)) + (pFaceData.pKeyFrames[lKey1].fRate * fLerpValue);
		}
		else
		{
			return pFaceData.pKeyFrames[lKey0].fRate;
		}	
	}

	public MmdMotionPlayer(MmdPmdModel i_pmd_model, MmdVmdMotion i_vmd_model)
	{
		this._ref_pmd_model = i_pmd_model;
		this._ref_vmd_motion = i_vmd_model;
		this.m_bLoop=false;
		
		PmdBone[] bone_array=i_pmd_model.getBoneArray();

		//スキニング用のmatrix/For the matrix skinning
		this._skinning_mat=MmdMatrix.createArray(bone_array.length);
		
		
		//---------------------------------------------------------
		// 操作対象ボーンのポインタを設定する/Set the pointer of the bone being manipulated
		MotionData[] pMotionDataList = i_vmd_model.refMotionDataArray();
		this.m_ppBoneList =new PmdBone[pMotionDataList.length];
		for(int i=0;i<pMotionDataList.length;i++)
		{
			this.m_ppBoneList[i]=i_pmd_model.getBoneByName(pMotionDataList[i].szBoneName);
		}
			

		//---------------------------------------------------------
		// 操作対象表情のポインタを設定する/Look set to operate on a pointer
		FaceData[] pFaceDataList = i_vmd_model.refFaceDataArray();
		this.m_ppFaceList = new PmdFace[pFaceDataList.length];
		for(int i=0;i<pFaceDataList.length;i++)
		{
			this.m_ppFaceList[i]=i_pmd_model.getFaceByName(pFaceDataList[i].szFaceName);
		}
		
		
		//首^H頭のボーンを探しておく/^ H you looking neck bone head
		this.m_pNeckBone=null;
		for(int i=0;i<bone_array.length;i++){
			if(bone_array[i].getName().equals("頭")){
				this.m_pNeckBone = bone_array[i];
				break;
			}			
		}		

		// 変数初期値設定/Variable initialization
		this.m_fOldFrame = this.m_fFrame = 0.0f;
		return;
	}

	private boolean updateBoneFace(float fElapsedFrame) throws MmdException
	{
		//---------------------------------------------------------
		// 指定フレームのデータでボーンを動かす/Bones move in the specified data frame
		final PmdBone[] ppBone = this.m_ppBoneList;
		MmdVector3 vec3Position=new MmdVector3();
		MmdVector4 vec4Rotate=new MmdVector4();

		MotionData[] pMotionDataList = _ref_vmd_motion.refMotionDataArray();
		for(int i=0;i<pMotionDataList.length;i++)
		{
			getMotionPosRot(pMotionDataList[i], m_fFrame,vec3Position,vec4Rotate);

			// 補間なし/No interpolation
			if(ppBone[i]==null){
				continue;
			}
			ppBone[i].m_vec3Position.setValue(vec3Position);
			ppBone[i].m_vec4Rotate.setValue(vec4Rotate);

			//	 補間あり/With interpolation
			//				Vector3Lerp( &((*pBone)->m_vec3Position), &((*pBone)->m_vec3Position), &vec3Position, fLerpValue );
			//				QuaternionSlerp( &((*pBone)->m_vec4Rotate), &((*pBone)->m_vec4Rotate), &vec4Rotate, fLerpValue );
		}

		//---------------------------------------------------------
		// 指定フレームのデータで表情を変形する/Transformed expression data of the specified frame
		MmdVector3[] position_array=this._ref_pmd_model.getPositionArray();
		
		PmdFace[] ppFace = this.m_ppFaceList;
		FaceData[] pFaceDataList = _ref_vmd_motion.refFaceDataArray();
		for(int i=0;i<pFaceDataList.length;i++)
		{
			final float fFaceRate = getFaceRate( pFaceDataList[i], m_fFrame );
			if(ppFace[i]==null){
				continue;
			}

			
			if( fFaceRate == 1.0f ){
				ppFace[i].setFace(position_array);
			}else if( 0.001f < fFaceRate ){
				ppFace[i].blendFace(position_array,fFaceRate );
			}
		}

		//---------------------------------------------------------
		// フレームを進める/Frame advance
		boolean bMotionFinshed = false;

		this.m_fOldFrame = this.m_fFrame;
		this.m_fFrame += fElapsedFrame;

		if(this.m_bLoop)
		{
			if( m_fOldFrame >=this._ref_vmd_motion.getMaxFrame() )
			{
				this.m_fOldFrame = 0.0f;
				this.m_fFrame = this.m_fFrame - this._ref_vmd_motion.getMaxFrame();
			}
		}

		if( this.m_fFrame >= this._ref_vmd_motion.getMaxFrame() )
		{
			this.m_fFrame = this._ref_vmd_motion.getMaxFrame();
			bMotionFinshed = true;
		}

		return bMotionFinshed;
	}
	public void setLoop(boolean i_is_loop)
	{
		this.m_bLoop=i_is_loop;
		return;
	}

	public void updateMotion( float fElapsedFrame ) throws MmdException
	{
		final MmdVector3[] position_array=this._ref_pmd_model.getPositionArray();
		final PmdIK[] ik_array=this._ref_pmd_model.getIKArray();
		final PmdBone[] bone_array=this._ref_pmd_model.getBoneArray();
		final PmdFace[] face_array=this._ref_pmd_model.getFaceArray();
		// モーション更新前に表情をリセット/Reset before updating the look of motion
		if(face_array!=null){
			face_array[0].setFace(position_array);
		}

		// モーション更新/Motion Update
		updateBoneFace( fElapsedFrame );

		// ボーン行列の更新/Bone matrix update
		for(int i = 0 ; i < bone_array.length ; i++ )
		{
			bone_array[i].updateMatrix();
		}

		// IKの更新/IK Update
		for(int i = 0 ; i < ik_array.length ; i++ )
		{
			ik_array[i].update();
		}
		return;
	}
	/**
	 * look me
	 * @param pvec3LookTarget
	 */
	private MmdVector3 __updateNeckBone_looktarget=new MmdVector3();
	public void updateNeckBone(float i_x,float i_y,float i_z)
	{
		final MmdVector3 looktarget=this.__updateNeckBone_looktarget;
		looktarget.x=i_x;
		looktarget.y=i_y;
		looktarget.z=i_z;
		if(this.m_pNeckBone==null)
		{
			return;
		}
		this.m_pNeckBone.lookAt(looktarget);

		PmdBone[] bone_array=this._ref_pmd_model.getBoneArray();
		int i;
		for( i = 0 ; i < bone_array.length ; i++ )
		{
			if(this.m_pNeckBone ==bone_array[i] ){
				break;
			}
		}
		for( ; i < bone_array.length ; i++ )
		{
			bone_array[i].updateMatrix();
		}
		return;
	}	
	
	//現在のスキニングマトリクスを返す。/Returns the current skinning matrix.
	public MmdMatrix[] refSkinningMatrix()
	{
		PmdBone[] bone_array=this._ref_pmd_model.getBoneArray();
		// スキニング用行列の更新/For updating the matrix skinning
		for(int i = 0 ; i < bone_array.length ; i++ )
		{
			bone_array[i].updateSkinningMat(this._skinning_mat[i]);
		}

		return this._skinning_mat;
	}	
}
