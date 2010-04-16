package jp.nyatla.nymmd;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.*;
import org.lwjgl.BufferUtils;
import java.util.*;
import jp.nyatla.nymmd.types.*;

public class MmdPmdRenderLWJGL implements IMmdPmdRender
{
	private MmdPmdModel _ref_pmd;

	private GLMaterial[] _gl_materials;
	private MmdVector3[] _position_array;
	private MmdVector3[] _normal_array;

	private GLTextureListLWJGL _textures;

	public MmdPmdRenderLWJGL()
	{
		this._textures = new GLTextureListLWJGL();
		return;
	}

	public void dispose()
	{
		this._textures.reset();
	}

	public void setPmd(MmdPmdModel i_pmd, IMmdDataIo i_io) throws MmdException
	{
		this._textures.reset();
		this._ref_pmd=i_pmd;
		final int number_of_vertex=i_pmd.getNumberOfVertex();
		this._position_array = MmdVector3.createArray(number_of_vertex);
		this._normal_array = MmdVector3.createArray(number_of_vertex);

		PmdMaterial[] m = i_pmd.getMaterials();// this._ref_materials;
		Vector<GLMaterial> gl_materials = new Vector<GLMaterial>();
		for (int i = 0; i < m.length; i++) {
			final GLMaterial new_material = new GLMaterial();
			new_material.unknown=m[i].unknown;
			// D,A,S[rgba]
			m[i].col4Diffuse.getValue(new_material.color, 0);
			m[i].col4Ambient.getValue(new_material.color, 4);
			m[i].col4Specular.getValue(new_material.color,8);
			new_material.fShininess = m[i].fShininess;
			if (m[i].texture_name != null) {
				new_material.texture = this._textures.getTexture(m[i].texture_name, i_io);
			} else {
				new_material.texture = null;
			}
			new_material.indices=BufferUtils.createShortBuffer(m[i].indices.length);
			new_material.indices.put(m[i].indices);
			new_material.indices.flip();
			new_material.ulNumIndices = m[i].indices.length;
			gl_materials.add(new_material);
		}
		this._gl_materials = gl_materials.toArray(new GLMaterial[gl_materials.size()]);

		return;
	}

	private final MmdMatrix __tmp_matrix = new MmdMatrix();

	public void updateSkinning(MmdMatrix[] i_skinning_mat)
	{
		int number_of_vertex = this._ref_pmd.getNumberOfVertex();
		MmdVector3[] org_pos_array=this._ref_pmd.getPositionArray();
		MmdVector3[] org_normal_array=this._ref_pmd.getNormatArray();
		PmdSkinInfo[] org_skin_info=this._ref_pmd.getSkinInfoArray();

		final MmdMatrix matTemp = this.__tmp_matrix;
		for (int i = 0; i < number_of_vertex; i++) {
			if (org_skin_info[i].fWeight == 0.0f) {
				final MmdMatrix mat = i_skinning_mat[org_skin_info[i].unBoneNo[1]];
				this._position_array[i].Vector3Transform(org_pos_array[i], mat);
				this._normal_array[i].Vector3Rotate(org_normal_array[i], mat);
			} else if (org_skin_info[i].fWeight >= 0.9999f) {
				final MmdMatrix mat = i_skinning_mat[org_skin_info[i].unBoneNo[0]];
				this._position_array[i].Vector3Transform(org_pos_array[i], mat);
				this._normal_array[i].Vector3Rotate(org_normal_array[i], mat);
			} else {
				final MmdMatrix mat0 = i_skinning_mat[org_skin_info[i].unBoneNo[0]];
				final MmdMatrix mat1 = i_skinning_mat[org_skin_info[i].unBoneNo[1]];

				matTemp.MatrixLerp(mat0, mat1, org_skin_info[i].fWeight);

				this._position_array[i].Vector3Transform(org_pos_array[i], matTemp);
				this._normal_array[i].Vector3Rotate(org_normal_array[i], matTemp);
			}
		}
		return;
	}

	public void render()
	{
		final MmdTexUV[] texture_uv = this._ref_pmd.getUvArray();
		final int number_of_vertex = this._ref_pmd.getNumberOfVertex();
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		ByteBuffer pos_buf = ByteBuffer.allocateDirect(_position_array.length * 3 * 4);
		pos_buf.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < number_of_vertex; i++) {
			pos_buf.putFloat(_position_array[i].x);
			pos_buf.putFloat(_position_array[i].y);
			pos_buf.putFloat(_position_array[i].z);
		}
		ByteBuffer nom_array = ByteBuffer.allocateDirect(_position_array.length * 3 * 4);
		nom_array.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < number_of_vertex; i++) {
			nom_array.putFloat(_normal_array[i].x);
			nom_array.putFloat(_normal_array[i].y);
			nom_array.putFloat(_normal_array[i].z);
		}
		ByteBuffer tex_array = ByteBuffer.allocateDirect(texture_uv.length * 2 * 4);
		tex_array.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < number_of_vertex; i++) {
			tex_array.putFloat(texture_uv[i].u);
			tex_array.putFloat(texture_uv[i].v);
		}
		pos_buf.position(0);
		nom_array.position(0);
		tex_array.position(0);

		GL11.glVertexPointer(3, 0, pos_buf.asFloatBuffer());
		GL11.glNormalPointer(0, nom_array.asFloatBuffer());
		GL11.glTexCoordPointer(2,0, tex_array.asFloatBuffer());
		int vertex_index = 0;
		FloatBuffer diffuse = BufferUtils.createFloatBuffer(4);
		FloatBuffer ambient = BufferUtils.createFloatBuffer(4);
		FloatBuffer specular = BufferUtils.createFloatBuffer(4);
		for (int i = 0; i < this._gl_materials.length; i++) {
			diffuse.put(this._gl_materials[i].color[0]);
			diffuse.put(this._gl_materials[i].color[1]);
			diffuse.put(this._gl_materials[i].color[2]);
			diffuse.put(this._gl_materials[i].color[3]);
			ambient.put(this._gl_materials[i].color[4]);
			ambient.put(this._gl_materials[i].color[5]);
			ambient.put(this._gl_materials[i].color[6]);
			ambient.put(this._gl_materials[i].color[7]);
			specular.put(this._gl_materials[i].color[8]);
			specular.put(this._gl_materials[i].color[9]);
			specular.put(this._gl_materials[i].color[10]);
			specular.put(this._gl_materials[i].color[11]);
			diffuse.flip();
			ambient.flip();
			specular.flip();			
			GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, diffuse);
			GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT, ambient);
			GL11.glMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_SPECULAR, specular);
			GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, this._gl_materials[i].fShininess);

            if ((0x100 & this._gl_materials[i].unknown) == 0x100)
            {
            	GL11.glDisable(GL11.GL_CULL_FACE);
            }
            else
            {
            	GL11.glEnable(GL11.GL_CULL_FACE);
            }

			if (this._gl_materials[i].texture != null) {
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, this._gl_materials[i].texture.gl_texture_id);
			} else {
				GL11.glDisable(GL11.GL_TEXTURE_2D);
			}
			GL11.glDrawElements(GL11.GL_TRIANGLES,this._gl_materials[i].indices);
			vertex_index += this._gl_materials[i].ulNumIndices;
			diffuse.clear();ambient.clear();specular.clear();
		}

		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		return;
	}
class GLTextureData
{
	public int gl_texture_id;

	public String file_name;
}

class GLTextureListLWJGL
{
	private final ArrayList<GLTextureData> m_pTextureList = new ArrayList<GLTextureData>();

	public void reset()
	{
		for (int i = 0; i < m_pTextureList.size(); i++) {
			IntBuffer ids = BufferUtils.createIntBuffer(1);
			ids.put(this.m_pTextureList.get(i).gl_texture_id);
			GL11.glDeleteTextures(ids);
		}
		this.m_pTextureList.clear();
		return;
	}

	private GLTextureData createTexture(String szFileName, InputStream i_st) throws MmdException
	{
		BufferedImage img;
		try {
			img = ImageIO.read(i_st);
		} catch (Exception e) {
			throw new MmdException();
		}
		IntBuffer texid = BufferUtils.createIntBuffer(2);
		GL11.glGenTextures(texid);
		int gl_tex_id = texid.get(0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, gl_tex_id);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);

		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

		int[] rgb_array = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
		IntBuffer rgbwrap = BufferUtils.createIntBuffer(rgb_array.length);
			rgbwrap.put(rgb_array);
		rgbwrap.flip();
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, img.getWidth(), img.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgbwrap);
		FloatBuffer prio = BufferUtils.createFloatBuffer(1);
		prio.put(0, 1.0f);
		prio.flip();
		texid.flip();
		GL11.glPrioritizeTextures(texid, prio);
		GLTextureData ret = new GLTextureData();
		ret.file_name = szFileName;
		ret.gl_texture_id = gl_tex_id;
		
		return ret;
	}
	public GLTextureData getTexture(String i_filename, IMmdDataIo i_io) throws MmdException
	{
		GLTextureData ret;

		final int len = this.m_pTextureList.size();
		for (int i = 0; i < len; i++) {
			ret = this.m_pTextureList.get(i);
			if (ret.file_name.equalsIgnoreCase(i_filename)) {
				return ret;
			}
		}

		ret = createTexture(i_filename, i_io.request(i_filename));
		if (ret != null) {
			this.m_pTextureList.add(ret);
			return ret;
		}

		return null;

	}
}

class GLMaterial
{
	public final float[] color = new float[12];// Diffuse,Specular,Ambient
	public float fShininess;
	public ShortBuffer indices;
	public int ulNumIndices;
	public GLTextureData texture;
	public int unknown;
}
}