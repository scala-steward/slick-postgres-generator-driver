package org.hatdex.libs.dal

import slick.codegen.SourceCodeGenerator
import slick.model.Model
import slick.profile.SqlProfile.ColumnOption

class TypemappedPgCodeGenerator(model: Model) extends SourceCodeGenerator(model) {
  override def Table = new Table(_) { table =>
    override def Column = new Column(_) { column =>
      // customize db type -> scala type mapping, pls adjust it according to your environment
      override def rawType: String = model.tpe match {
        case "java.sql.Date" => "org.joda.time.LocalDate"
        case "java.sql.Time" => "org.joda.time.LocalTime"
        case "java.sql.Timestamp" => model.options.find(_.isInstanceOf[ColumnOption.SqlType]).map(_.asInstanceOf[ColumnOption.SqlType].typeName).map({
          case "timestamp"   => "org.joda.time.LocalDateTime"
          case "timestamptz" => "org.joda.time.DateTime"
          case _             => "org.joda.time.LocalDateTime"
        }).getOrElse("org.joda.time.LocalDateTime")
        // currently, all types that's not built-in support were mapped to `String`
        case "String" => model.options.find(_.isInstanceOf[ColumnOption.SqlType]).map(_.asInstanceOf[ColumnOption.SqlType].typeName).map({
          case "hstore"   => "Map[String, String]"
          case "geometry" => "com.vividsolutions.jts.geom.Geometry"
          case "int8[]"   => "List[Long]"
          case "int4[]"   => "List[Int]"
          case "text[]"   => "List[String]"
          case "varchar"  => "String"
          case "_int4"    => "List[Int]"
          case "_text"    => "List[String]"
          case "jsonb"    => "play.api.libs.json.JsValue"
          case "_jsonb"   => "List[play.api.libs.json.JsValue]"
          case _          => "String"
        }).getOrElse("String")
        case "scala.collection.Seq" => model.options.find(_.isInstanceOf[ColumnOption.SqlType]).map(_.asInstanceOf[ColumnOption.SqlType].typeName).map({
          case "_text" => "List[String]"
          case _       => "String"
        }).getOrElse("String")
        case _ => super.rawType
      }
    }
  }

  // ensure to use our customized postgres driver at `import profile.simple._`
  override def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]): String = {
    s"""
    |package $pkg
    |// AUTO-GENERATED Slick data model
    |/** Stand-alone Slick data model for immediate use */
    |object $container extends {
    |  val profile = $profile
    |} with $container
    |/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
    |trait $container {
    | val profile: $profile
    | import profile.api._
    | ${indent(code)}
    |}
      """.stripMargin.trim()
  }
}
