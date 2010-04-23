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

import java.util.*;

import java.util.Arrays;
import java.io.*;


import jp.nyatla.nymmd.struct.DataReader;
import jp.nyatla.nymmd.struct.vmd.VMD_Face;
import jp.nyatla.nymmd.struct.vmd.VMD_Header;
import jp.nyatla.nymmd.struct.vmd.VMD_Motion;
import jp.nyatla.nymmd.types.*;

//------------------------------
//ボーンキーフレームソート用比較関数/For comparing BoneKeyFrames
//------------------------------
class BoneCompare implements java.util.Comparator<BoneKeyFrame>
{
	public int compare(BoneKeyFrame o1, BoneKeyFrame o2)
	{
		return (int)(o1.fFrameNo - o2.fFrameNo);
	}
}


//------------------------------
//表情キーフレームソート用比較関数/For Comparing FaceKeyFrames
//------------------------------
class FaceCompare implements java.util.Comparator<FaceKeyFrame>
{
	public int compare(FaceKeyFrame o1, FaceKeyFrame o2)
	{
		return (int)(o1.fFrameNo - o2.fFrameNo);
	}
}

public class MmdVmdMotion
{
	private MotionData[] _motion_data_array;	// ボーンごとのキーフレームデータのリスト/Keyframe per Bone array
	private FaceData[] _face_data_array;	// 表情ごとのキーフレームデータのリスト/Keyframe FaceData Array 
	private float m_fMaxFrame;		// 最後のフレーム番号/The last frame number

	public MmdVmdMotion(InputStream i_stream) throws MmdException
	{
		initialize(i_stream);
		return;
	}

	public MotionData[] refMotionDataArray()
	{
		return this._motion_data_array;
	}
	public FaceData[] refFaceDataArray()
	{
		return this._face_data_array;
	}

	public float getMaxFrame()
	{
		return this.m_fMaxFrame;
	}
	

	private boolean initialize(InputStream i_st) throws MmdException
	{
		
		DataReader reader=new DataReader(i_st);

		// ヘッダのチェック/Header check
		VMD_Header tmp_vmd_header=new VMD_Header();
		tmp_vmd_header.read(reader);
		if(!tmp_vmd_header.szHeader.equalsIgnoreCase("Vocaloid Motion Data 0002"))
		{
			throw new MmdException("Header of VMD not \"Vocaloid Motion Data 0002\"");
		}
		//ボーンと最大フレームを取得/Get the bones and maximum frame
		float[] max_frame=new float[1];
		this._motion_data_array=createMotionDataList(reader,max_frame);
		this.m_fMaxFrame=max_frame[0];
		
		//表情と最大フレームを再取得/And look to regain the maximum frame
		this._face_data_array=createFaceDataList(reader,max_frame);
		this.m_fMaxFrame=this.m_fMaxFrame>max_frame[0]?this.m_fMaxFrame:max_frame[0];
		
		return true;
	}
	
	private static FaceData[] createFaceDataList(DataReader i_reader,float[] o_max_frame) throws MmdException
	{
		//-----------------------------------------------------
		// 表情のキーフレーム数を取得/Get the FaceData keyframes
		Vector<FaceData> result=new Vector<FaceData>();
		int ulNumFaceKeyFrames=i_reader.readInt();	

		//規定フレーム数分表情を読み込み/Minutes reading frames look Policies
		VMD_Face[] tmp_vmd_face=new VMD_Face[ulNumFaceKeyFrames];
		for(int i=0;i<ulNumFaceKeyFrames;i++){
			tmp_vmd_face[i]= new VMD_Face();
			tmp_vmd_face[i].read(i_reader);
		}
		float max_frame=0.0f;
		for(int i = 0 ; i < ulNumFaceKeyFrames ; i++)
		{
			if(max_frame < (float)tmp_vmd_face[i].ulFrameNo ){
				max_frame = (float)tmp_vmd_face[i].ulFrameNo;	// 最大フレーム更新/Update maximum frame
			}
			boolean is_found=false;
			for(int i2=0;i2<result.size();i2++)
			{
				final FaceData pFaceTemp = result.get(i2);
				if(pFaceTemp.szFaceName.equals(tmp_vmd_face[i].szFaceName))
				{
					// リストに追加済み/Added to the list
					pFaceTemp.ulNumKeyFrames++;
					is_found=true;
					break;
				}
			}

			if(!is_found)
			{
				// リストにない場合は新規ノードを追加/If you do not add new nodes to the list
				FaceData pNew = new FaceData();
				pNew.szFaceName=tmp_vmd_face[i].szFaceName;
				pNew.ulNumKeyFrames = 1;
				result.add(pNew);
			}
		}

		// キーフレーム配列を確保/Secure key frame sequence
		for(int i=0;i<result.size();i++)
		{
			FaceData pFaceTemp=result.get(i);
			pFaceTemp.pKeyFrames = FaceKeyFrame.createArray(pFaceTemp.ulNumKeyFrames);
			pFaceTemp.ulNumKeyFrames = 0;		// 配列インデックス用にいったん0にする/Array index to zero
		}
		
		// 表情ごとにキーフレームを格納/Each keyframe contains FaceData
		for(int i = 0 ; i < ulNumFaceKeyFrames ; i++)
		{
			for(int i2=0;i2<result.size();i2++)
			{
				FaceData pFaceTemp = result.get(i2);
				if(pFaceTemp.szFaceName.equals(tmp_vmd_face[i].szFaceName))
				{
					FaceKeyFrame pKeyFrame = pFaceTemp.pKeyFrames[pFaceTemp.ulNumKeyFrames];

					pKeyFrame.fFrameNo = (float)tmp_vmd_face[i].ulFrameNo;
					pKeyFrame.fRate    =        tmp_vmd_face[i].fFactor;

					pFaceTemp.ulNumKeyFrames++;
					break;
				}
			}
		}

		// キーフレーム配列を昇順にソート/Sort ascending sequence keyframe
		for(int i=0;i<result.size();i++)
		{
			FaceData pFaceTemp = result.get(i);
			Arrays.sort(pFaceTemp.pKeyFrames, new FaceCompare());
		}
		o_max_frame[0]=max_frame;
		return result.toArray(new FaceData[result.size()]);
	}
	private static MotionData[] createMotionDataList(DataReader i_reader,float[] o_max_frame) throws MmdException
	{	
		Vector<MotionData> result=new Vector<MotionData>();
		// まずはモーションデータ中のボーンごとのキーフレーム数をカウント
		//Count the number of key frames for each bone in the first motion data
		final int ulNumBoneKeyFrames=i_reader.readInt();


		//ボーンを指定数読み込み/Specify the number of bone loading
		VMD_Motion[] tmp_vmd_motion=new VMD_Motion[ulNumBoneKeyFrames];
		for(int i=0;i<ulNumBoneKeyFrames;i++){
			tmp_vmd_motion[i]= new VMD_Motion();
			tmp_vmd_motion[i].read(i_reader);
		}		
		
		float max_frame=0.0f;

		for(int i = 0 ; i < ulNumBoneKeyFrames ; i++)
		{
			if( max_frame < tmp_vmd_motion[i].ulFrameNo){
				max_frame = tmp_vmd_motion[i].ulFrameNo;	// 最大フレーム更新/Update maximum frame
			}
			boolean is_found=false;
			for(int i2=0;i2<result.size();i2++)
			{
				final MotionData pMotTemp = result.get(i2);
				if(pMotTemp.szBoneName.equals(tmp_vmd_motion[i].szBoneName))
				{
					// リストに追加済みのボーン/Added to the list of bones
					pMotTemp.ulNumKeyFrames++;
					is_found=true;
					break;
				}
			}
			
			if(!is_found)
			{
				// リストにない場合は新規ノードを追加/If you do not add new nodes to the list
				MotionData pNew = new MotionData();
				pNew.szBoneName=tmp_vmd_motion[i].szBoneName;
				pNew.ulNumKeyFrames = 1;
				result.add(pNew);
			}
		}

		
		// キーフレーム配列を確保/Secure key frame sequence
		for(int i=0;i<result.size();i++)
		{
			final MotionData pMotTemp = result.get(i);
			pMotTemp.pKeyFrames = BoneKeyFrame.createArray(pMotTemp.ulNumKeyFrames);
			pMotTemp.ulNumKeyFrames = 0;		// 配列インデックス用にいったん0にする/Array index to zero
		}
		
		// ボーンごとにキーフレームを格納/Each keyframe contains bone
		for(int i = 0 ; i < ulNumBoneKeyFrames ; i++)
		{
			for(int i2=0;i2<result.size();i2++)
			{
				final MotionData pMotTemp = result.get(i2);
				if(pMotTemp.szBoneName.equals(tmp_vmd_motion[i].szBoneName))
				{
					final BoneKeyFrame pKeyFrame = pMotTemp.pKeyFrames[pMotTemp.ulNumKeyFrames];

					pKeyFrame.fFrameNo     = (float)tmp_vmd_motion[i].ulFrameNo;
					pKeyFrame.vec3Position.setValue(tmp_vmd_motion[i].vec3Position);
					pKeyFrame.vec4Rotate.QuaternionNormalize(tmp_vmd_motion[i].vec4Rotate);

					pMotTemp.ulNumKeyFrames++;

					break;
				}
			}
		}

		// キーフレーム配列を昇順にソート/Sort ascending sequence keyframe

		for(int i=0;i<result.size();i++)
		{
			final MotionData pMotTemp = result.get(i);
			Arrays.sort(pMotTemp.pKeyFrames, new BoneCompare());
		}
		
		o_max_frame[0]=max_frame;
		return result.toArray(new MotionData[result.size()]);
		
	}
}
